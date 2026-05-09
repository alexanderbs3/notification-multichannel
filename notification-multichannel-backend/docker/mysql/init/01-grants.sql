CREATE DATABASE IF NOT EXISTS notification_multichannel;

CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED BY 'notification_password';
GRANT ALL PRIVILEGES ON notification_multichannel.* TO 'notification_user'@'%';
FLUSH PRIVILEGES;
