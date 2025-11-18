import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import Stripe from "stripe";

dotenv.config();

const app = express();
const port = process.env.PORT || 4242;

if (!process.env.STRIPE_SECRET_KEY) {
  throw new Error("STRIPE_SECRET_KEY is required");
}

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, {
  apiVersion: "2023-10-16",
});

app.use(cors({ origin: true }));

app.post("/webhook", express.raw({ type: "application/json" }), (req, res) => {
  const signature = req.headers["stripe-signature"];
  if (!process.env.STRIPE_WEBHOOK_SECRET) {
    return res.status(500).json({ error: "STRIPE_WEBHOOK_SECRET not configured" });
  }

  let event;
  try {
    event = stripe.webhooks.constructEvent(req.body, signature, process.env.STRIPE_WEBHOOK_SECRET);
  } catch (err) {
    return res.status(400).json({ error: `Webhook Error: ${err.message}` });
  }

  switch (event.type) {
    case "payment_intent.succeeded":
    case "payment_intent.payment_failed":
    case "payment_intent.processing":
      break;
    default:
      break;
  }

  res.json({ received: true });
});

app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({ status: "ok" });
});

app.post("/create-payment-intent", async (req, res) => {
  try {
    const { amount, currency = "lkr", customerId, metadata } = req.body || {};

    const normalizedAmount = normalizeAmount(amount);
    if (!normalizedAmount) {
      return res.status(400).json({ error: "Amount must be a positive integer" });
    }

    const customer = await resolveCustomer(customerId);
    const ephemeralKey = await stripe.ephemeralKeys.create(
      { customer },
      { apiVersion: "2023-10-16" }
    );

    const paymentIntent = await stripe.paymentIntents.create({
      amount: normalizedAmount,
      currency: (currency || "lkr").toLowerCase(),
      customer,
      automatic_payment_methods: { enabled: true },
      metadata: sanitizeMetadata(metadata),
    });

    res.json({
      publishableKey: process.env.STRIPE_PUBLISHABLE_KEY,
      customerId: customer,
      ephemeralKeySecret: ephemeralKey.secret,
      paymentIntentClientSecret: paymentIntent.client_secret,
    });
  } catch (error) {
    res.status(500).json({ error: error.message || "Stripe error" });
  }
});

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`Stripe server listening on port ${port}`);
});

function normalizeAmount(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return 0;
  }
  const rounded = Math.round(parsed);
  return rounded > 0 ? rounded : 0;
}

async function resolveCustomer(candidateId) {
  if (candidateId) {
    try {
      const existing = await stripe.customers.retrieve(candidateId);
      if (!existing.deleted) {
        return existing.id;
      }
    } catch (err) {
      if (err?.statusCode !== 404) {
        throw err;
      }
    }
  }
  const customer = await stripe.customers.create();
  return customer.id;
}

function sanitizeMetadata(value) {
  if (!value || typeof value !== "object") {
    return undefined;
  }
  const result = {};
  for (const [key, val] of Object.entries(value)) {
    if (typeof val === "string" || typeof val === "number" || typeof val === "boolean") {
      result[key] = String(val).slice(0, 500);
    }
  }
  return result;
}
