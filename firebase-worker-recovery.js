/**
 * Firebase Worker Data Recovery Script
 * Run this script using: firebase firestore:delete --all-collections (for backup first)
 * Then run: node firebase-worker-recovery.js
 */

const admin = require('firebase-admin');
const serviceAccount = require('./path/to/your/service-account-key.json');

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://your-project-id.firebaseio.com'
});

const db = admin.firestore();

// Required fields for worker documents
const REQUIRED_WORKER_FIELDS = [
  'uid', 'email', 'displayName', 'role', 'createdAt', 'phone',
  'emailVerified', 'avatarUri', 'location', 'bio'
];

// Required fields for service documents
const REQUIRED_SERVICE_FIELDS = [
  'id', 'category', 'name', 'bio', 'priceType', 'priceValue',
  'imageUri', 'ownerId', 'ownerName', 'ownerEmail', 'updatedAt',
  'serviceArea', 'latitude', 'longitude', 'coverageRadiusKm'
];

/**
 * Main recovery function
 */
async function recoverAllWorkerData() {
  console.log('🚀 Starting worker data recovery...');
  
  try {
    // Step 1: Get all workers
    const workersSnapshot = await db.collection('users')
      .where('role', '==', 'WORKER')
      .get();
    
    console.log(`📊 Found ${workersSnapshot.size} workers to process`);
    
    let recoveredWorkers = 0;
    let recoveredServices = 0;
    
    // Step 2: Process each worker
    for (const workerDoc of workersSnapshot.docs) {
      const workerId = workerDoc.id;
      const workerData = workerDoc.data();
      
      console.log(`\n🔧 Processing worker: ${workerId}`);
      
      // Recover worker document
      const workerRecovery = await recoverWorkerDocument(workerId, workerData);
      if (workerRecovery.recovered) {
        recoveredWorkers++;
        console.log(`  ✅ Recovered worker fields: ${workerRecovery.fields.join(', ')}`);
      }
      
      // Recover worker services
      const serviceRecovery = await recoverWorkerServices(workerId);
      recoveredServices += serviceRecovery.recoveredCount;
      
      if (serviceRecovery.recoveredCount > 0) {
        console.log(`  ✅ Recovered ${serviceRecovery.recoveredCount} services`);
      }
    }
    
    console.log(`\n🎉 Recovery completed!`);
    console.log(`📈 Workers recovered: ${recoveredWorkers}`);
    console.log(`📈 Services recovered: ${recoveredServices}`);
    
  } catch (error) {
    console.error('❌ Recovery failed:', error);
  } finally {
    await admin.app().delete();
  }
}

/**
 * Recover a single worker document
 */
async function recoverWorkerDocument(workerId, workerData) {
  const updates = {};
  
  // Check each required field
  for (const field of REQUIRED_WORKER_FIELDS) {
    if (!workerData.hasOwnProperty(field) || workerData[field] === null || workerData[field] === undefined) {
      updates[field] = getDefaultValueForWorkerField(field, workerData, workerId);
    }
  }
  
  // Update if there are missing fields
  if (Object.keys(updates).length > 0) {
    await db.collection('users').doc(workerId).update(updates);
    return {
      recovered: true,
      fields: Object.keys(updates)
    };
  }
  
  return { recovered: false, fields: [] };
}

/**
 * Recover all services for a worker
 */
async function recoverWorkerServices(workerId) {
  // Get all services for this worker
  const servicesSnapshot = await db.collection('services')
    .where('ownerId', '==', workerId)
    .get();
  
  let recoveredCount = 0;
  
  for (const serviceDoc of servicesSnapshot.docs) {
    const serviceData = serviceDoc.data();
    const updates = {};
    
    // Check each required field
    for (const field of REQUIRED_SERVICE_FIELDS) {
      if (!serviceData.hasOwnProperty(field) || serviceData[field] === null || serviceData[field] === undefined) {
        updates[field] = getDefaultValueForServiceField(field, serviceData, workerId, serviceDoc.id);
      }
    }
    
    // Update service if there are missing fields
    if (Object.keys(updates).length > 0) {
      await db.collection('services').doc(serviceDoc.id).update(updates);
      recoveredCount++;
    }
    
    // Ensure service exists in worker subcollection
    await ensureServiceInWorkerSubcollection(workerId, serviceDoc.id, serviceData);
  }
  
  return { recoveredCount };
}

/**
 * Ensure service exists in worker's subcollection
 */
async function ensureServiceInWorkerSubcollection(workerId, serviceId, serviceData) {
  const workerServiceRef = db.collection('workers').doc(workerId).collection('services').doc(serviceId);
  const workerServiceDoc = await workerServiceRef.get();
  
  if (!workerServiceDoc.exists) {
    await workerServiceRef.set(serviceData);
    console.log(`  📋 Added service ${serviceId} to worker subcollection`);
  }
}

/**
 * Get default value for missing worker field
 */
function getDefaultValueForWorkerField(field, workerData, workerId) {
  switch (field) {
    case 'uid':
      return workerId;
    case 'email':
      return workerData.email || null;
    case 'displayName':
      return workerData.displayName || 'Unknown Worker';
    case 'role':
      return 'WORKER';
    case 'createdAt':
      return workerData.createdAt || admin.firestore.FieldValue.serverTimestamp();
    case 'phone':
      return workerData.phone || null;
    case 'emailVerified':
      return workerData.emailVerified || false;
    case 'avatarUri':
      return workerData.avatarUri || null;
    case 'location':
      return workerData.location || null;
    case 'bio':
      return workerData.bio || null;
    default:
      return null;
  }
}

/**
 * Get default value for missing service field
 */
function getDefaultValueForServiceField(field, serviceData, workerId, serviceId) {
  switch (field) {
    case 'id':
      return serviceId;
    case 'category':
      return serviceData.category || 'general';
    case 'name':
      return serviceData.name || 'Service';
    case 'bio':
      return serviceData.bio || 'Professional service';
    case 'priceType':
      return serviceData.priceType || 'CUSTOM';
    case 'priceValue':
      return serviceData.priceValue || '0';
    case 'imageUri':
      return serviceData.imageUri || null;
    case 'ownerId':
      return workerId;
    case 'ownerName':
      return serviceData.ownerName || null;
    case 'ownerEmail':
      return serviceData.ownerEmail || null;
    case 'updatedAt':
      return admin.firestore.FieldValue.serverTimestamp();
    case 'serviceArea':
      return serviceData.serviceArea || 'Colombo';
    case 'latitude':
      return serviceData.latitude || 6.927079;
    case 'longitude':
      return serviceData.longitude || 79.861244;
    case 'coverageRadiusKm':
      return serviceData.coverageRadiusKm || 20.0;
    default:
      return null;
  }
}

/**
 * Validate all worker documents (without fixing)
 */
async function validateWorkerData() {
  console.log('🔍 Validating worker data...');
  
  try {
    const workersSnapshot = await db.collection('users')
      .where('role', '==', 'WORKER')
      .get();
    
    console.log(`📊 Found ${workersSnapshot.size} workers to validate`);
    
    const validationReport = {
      totalWorkers: workersSnapshot.size,
      workersWithIssues: 0,
      issues: []
    };
    
    for (const workerDoc of workersSnapshot.docs) {
      const workerId = workerDoc.id;
      const workerData = workerDoc.data();
      
      const workerIssues = {
        workerId,
        missingUserFields: [],
        missingServiceFields: {}
      };
      
      // Check user document fields
      for (const field of REQUIRED_WORKER_FIELDS) {
        if (!workerData.hasOwnProperty(field) || workerData[field] === null || workerData[field] === undefined) {
          workerIssues.missingUserFields.push(field);
        }
      }
      
      // Check services
      const servicesSnapshot = await db.collection('services')
        .where('ownerId', '==', workerId)
        .get();
      
      for (const serviceDoc of servicesSnapshot.docs) {
        const serviceData = serviceDoc.data();
        const missingServiceFields = [];
        
        for (const field of REQUIRED_SERVICE_FIELDS) {
          if (!serviceData.hasOwnProperty(field) || serviceData[field] === null || serviceData[field] === undefined) {
            missingServiceFields.push(field);
          }
        }
        
        if (missingServiceFields.length > 0) {
          workerIssues.missingServiceFields[serviceDoc.id] = missingServiceFields;
        }
      }
      
      if (workerIssues.missingUserFields.length > 0 || Object.keys(workerIssues.missingServiceFields).length > 0) {
        validationReport.workersWithIssues++;
        validationReport.issues.push(workerIssues);
      }
    }
    
    console.log('\n📋 Validation Report:');
    console.log(`Total workers: ${validationReport.totalWorkers}`);
    console.log(`Workers with issues: ${validationReport.workersWithIssues}`);
    
    if (validationReport.issues.length > 0) {
      console.log('\n⚠️ Issues found:');
      validationReport.issues.forEach(issue => {
        console.log(`\nWorker ${issue.workerId}:`);
        if (issue.missingUserFields.length > 0) {
          console.log(`  Missing user fields: ${issue.missingUserFields.join(', ')}`);
        }
        if (Object.keys(issue.missingServiceFields).length > 0) {
          console.log('  Missing service fields:');
          Object.entries(issue.missingServiceFields).forEach(([serviceId, fields]) => {
            console.log(`    Service ${serviceId}: ${fields.join(', ')}`);
          });
        }
      });
    } else {
      console.log('✅ All worker documents are valid!');
    }
    
  } catch (error) {
    console.error('❌ Validation failed:', error);
  } finally {
    await admin.app().delete();
  }
}

// Command line interface
const command = process.argv[2];

if (command === 'validate') {
  validateWorkerData();
} else if (command === 'recover') {
  recoverAllWorkerData();
} else {
  console.log('Usage:');
  console.log('  node firebase-worker-recovery.js validate  - Validate worker data');
  console.log('  node firebase-worker-recovery.js recover   - Recover worker data');
  console.log('');
  console.log('Before running, make sure to:');
  console.log('1. Replace service-account-key.json path');
  console.log('2. Update your project ID in the databaseURL');
  console.log('3. Backup your data using Firebase Console');
}
