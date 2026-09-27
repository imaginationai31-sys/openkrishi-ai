import { getMessaging, getToken, isSupported, onMessage } from "firebase/messaging";
import { app } from "../firebase";

const VAPID_KEY = import.meta.env.VITE_FIREBASE_VAPID_KEY || "";

export async function requestPushNotifications(): Promise<string | null> {
  if (!VAPID_KEY || !("Notification" in window) || Notification.permission === "denied") return null;
  if (!(await isSupported())) return null;
  const permission = Notification.permission === "granted"
    ? "granted"
    : await Notification.requestPermission();
  if (permission !== "granted") return null;

  const registration = await navigator.serviceWorker.register("/firebase-messaging-sw.js");
  const messaging = getMessaging(app);
  return getToken(messaging, { vapidKey: VAPID_KEY, serviceWorkerRegistration: registration });
}

export async function listenForForegroundMessages(onNotification: (title: string, body: string) => void) {
  if (!VAPID_KEY || !(await isSupported())) return () => {};
  const messaging = getMessaging(app);
  return onMessage(messaging, (payload) => {
    const title = payload.notification?.title || "OpenKrishi AI";
    const body = payload.notification?.body || "You have a new farm alert.";
    onNotification(title, body);
  });
}
