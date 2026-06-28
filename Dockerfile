FROM python:3.9-slim

WORKDIR /app

# تثبيت المتطلبات
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# نسخ ملفات التطبيق
COPY backend/ .

# إنشاء مجلد السجلات
RUN mkdir -p /app/logs

# متغيرات البيئة
ENV BOT_TOKEN=""
ENV CHAT_ID=""
ENV API_PORT=5000
ENV DATABASE_PATH="/app/bot_data.db"
ENV LOG_PATH="/app/logs/"

EXPOSE 5000

# تشغيل البوت
CMD ["python", "bot.py"]
