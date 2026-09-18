export default async function handler(req, res) {
  if (req.method !== "GET") {
    return res.status(405).json({
      ok: false,
      error: "Method not allowed"
    });
  }

  try {
    const token = process.env.TELEGRAM_BOT_TOKEN;
    const chatId = process.env.TELEGRAM_CHAT_ID;

    if (!token || !chatId) {
      return res.status(500).json({
        ok: false,
        error: "Konfigurasi Telegram belum lengkap"
      });
    }

    const offset = Number(req.query.offset || 0);

    const response = await fetch(
      `https://api.telegram.org/bot${token}/getUpdates?offset=${offset}&limit=10&timeout=0`
    );

    const data = await response.json();

    if (!data.ok) {
      return res.status(500).json({
        ok: false,
        error: "Telegram API gagal"
      });
    }

    const updates = (data.result || [])
      .filter(update =>
        update.message?.chat?.id?.toString() === chatId
      )
      .map(update => ({
        update_id: update.update_id,
        text: update.message?.text || ""
      }));

    return res.status(200).json({
      ok: true,
      updates
    });

  } catch (error) {
    return res.status(500).json({
      ok: false,
      error: "Server error"
    });
  }
}
