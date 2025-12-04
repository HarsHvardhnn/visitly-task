-- MySQL initialization script for Docker
-- This script runs when the MySQL container starts for the first time

-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS rbac_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to the rbac_user
GRANT ALL PRIVILEGES ON rbac_db.* TO 'rbac_user'@'%';
FLUSH PRIVILEGES;

-- Use the database
USE rbac_db;

-- The tables will be created by Liquibase when the Spring Boot application starts

