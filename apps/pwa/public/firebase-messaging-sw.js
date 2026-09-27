importScripts("https://www.gstatic.com/firebasejs/12.5.0/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/12.5.0/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyBfI46YJApVNyiV4saVGO-OVsH9UgdDXfs",
  authDomain: "openkrishi-ai.firebaseapp.com",
  projectId: "openkrishi-ai",
  storageBucket: "openkrishi-ai.firebasestorage.app",
  messagingSenderId: "837358973413",
  appId: "1:837358973413:web:49f86614fd155f29ce3f0c"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || "OpenKrishi AI";
  const options = {
    body: payload.notification?.body || "You have a new farm alert.",
    icon: "/icon-192.png",
    badge: "/icon-192.png",
    data: payload.data || {}
  };
  self.registration.showNotification(title, options);
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
    for (const client of clientList) {
      if ("focus" in client) return client.focus();
    }
    if (clients.openWindow) return clients.openWindow("/");
    return undefined;
  }));
});
