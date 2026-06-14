import { Container, getContainer } from "@cloudflare/containers";

type Env = {
  WAKEVE_BACKEND: DurableObjectNamespace<WakeveBackendContainer>;
  ENVIRONMENT: string;
  JWT_SECRET: string;
  JWT_ISSUER: string;
  JWT_AUDIENCE: string;
  GOOGLE_CLIENT_ID?: string;
  GOOGLE_CLIENT_SECRET?: string;
  GOOGLE_REDIRECT_URI?: string;
  APPLE_CLIENT_ID?: string;
  APPLE_TEAM_ID?: string;
  APPLE_KEY_ID?: string;
  APPLE_PRIVATE_KEY?: string;
  APPLE_REDIRECT_URI?: string;
  FCM_PROJECT_ID?: string;
  FCM_SERVER_KEY?: string;
  APNS_KEY_ID?: string;
  APNS_TEAM_ID?: string;
  APNS_BUNDLE_ID?: string;
  APNS_AUTH_KEY?: string;
  APNS_ENVIRONMENT?: string;
  METRICS_WHITELIST_IPS?: string;
};

const INSTANCE_NAME = "app-review-production";

export class WakeveBackendContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";
}

function containerEnvVars(env: Env): Record<string, string> {
  const values: Record<string, string | undefined> = {
    ENVIRONMENT: env.ENVIRONMENT,
    JWT_SECRET: env.JWT_SECRET,
    JWT_ISSUER: env.JWT_ISSUER,
    JWT_AUDIENCE: env.JWT_AUDIENCE,
    GOOGLE_CLIENT_ID: env.GOOGLE_CLIENT_ID,
    GOOGLE_CLIENT_SECRET: env.GOOGLE_CLIENT_SECRET,
    GOOGLE_REDIRECT_URI: env.GOOGLE_REDIRECT_URI,
    APPLE_CLIENT_ID: env.APPLE_CLIENT_ID,
    APPLE_TEAM_ID: env.APPLE_TEAM_ID,
    APPLE_KEY_ID: env.APPLE_KEY_ID,
    APPLE_PRIVATE_KEY: env.APPLE_PRIVATE_KEY,
    APPLE_REDIRECT_URI: env.APPLE_REDIRECT_URI,
    FCM_PROJECT_ID: env.FCM_PROJECT_ID,
    FCM_SERVER_KEY: env.FCM_SERVER_KEY,
    APNS_KEY_ID: env.APNS_KEY_ID,
    APNS_TEAM_ID: env.APNS_TEAM_ID,
    APNS_BUNDLE_ID: env.APNS_BUNDLE_ID,
    APNS_AUTH_KEY: env.APNS_AUTH_KEY,
    APNS_ENVIRONMENT: env.APNS_ENVIRONMENT,
    METRICS_WHITELIST_IPS: env.METRICS_WHITELIST_IPS
  };

  return Object.fromEntries(
    Object.entries(values).filter((entry): entry is [string, string] => {
      const value = entry[1];
      return typeof value === "string" && value.length > 0;
    })
  );
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (!env.JWT_SECRET) {
      return new Response("Backend is missing JWT_SECRET", { status: 503 });
    }

    const container = getContainer(env.WAKEVE_BACKEND, INSTANCE_NAME);
    await container.start({
      envVars: containerEnvVars(env)
    });
    await container.startAndWaitForPorts(8080);

    return container.fetch(request);
  }
};
