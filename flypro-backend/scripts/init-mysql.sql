-- À exécuter une seule fois dans MySQL Workbench (connexion root)
CREATE DATABASE IF NOT EXISTS flypro_analytics
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'flypro'@'localhost' IDENTIFIED BY 'flypro';
CREATE USER IF NOT EXISTS 'flypro'@'%' IDENTIFIED BY 'flypro';

GRANT ALL PRIVILEGES ON flypro_analytics.* TO 'flypro'@'localhost';
GRANT ALL PRIVILEGES ON flypro_analytics.* TO 'flypro'@'%';

FLUSH PRIVILEGES;
