export default async function handler(req, res) {
  try {
    const token = process.env.TELEGRAM_BOT_TOKEN;

    if (!token) {
      return res.status(500).json({
        ok: false,
        error: "TELEGRAM_BOT_TOKEN belum tersedia"
      });
    }

    const response = await fetch(
      `https://api.telegram.org/bot${token}/getUpdates`
    );

    const data = await response.json();

    if (!data.ok) {
      return res.status(500).json({
        ok: false,
        error: "Gagal mengambil update Telegram"
      });
    }

    const updates = data.result || [];

    const result = updates.map(update => ({
      update_id: update.update_id,
      chat_id:
        update.message?.chat?.id ??
        update.edited_message?.chat?.id ??
        null,
      username:
        update.message?.from?.username ??
        update.edited_message?.from?.username ??
        null,
      text:
        update.message?.text ??
        update.edited_message?.text ??
        null
    }));

    return res.status(200).json({
      ok: true,
      updates: result
    });

  } catch (error) {
    return res.status(500).json({
      ok: false,
      error: "Server error"
    });
  }
}
