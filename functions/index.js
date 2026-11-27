const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

exports.notifyChatMember = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const chatId = event.params.chatId;
    if (!message || !message.senderId) return;
    const db = getFirestore();
    const chatSnap = await db.collection("chats").doc(chatId).get();
    const members = chatSnap.get("members") || [];
    const recipients = members.filter((uid) => uid !== message.senderId);
    if (!recipients.length) return;

    const users = await Promise.all(
      recipients.map((uid) => db.collection("users").doc(uid).get())
    );
    const tokens = users.map((user) => user.get("fcmToken")).filter(Boolean);
    if (!tokens.length) return;

    const sender = await db.collection("users").doc(message.senderId).get();
    const senderName = sender.get("displayName") || "رسالة جديدة";
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: senderName,
        body: message.type === "text" ? (message.text || "رسالة جديدة") : "أرسل وسائط إليك",
      },
      data: { chatId, senderId: message.senderId },
      android: { priority: "high", notification: { channelId: "wisal_messages", sound: "default" } },
    });
  }
);
