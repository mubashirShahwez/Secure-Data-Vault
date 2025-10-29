# 🔐 Secure Data Vault

A desktop application for securely storing sensitive text secrets and encrypted files using AES-256 encryption, PBKDF2 password hashing, and MySQL persistence.

## 📋 Table of Contents
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Security Architecture](#security-architecture)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Database Schema](#database-schema)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Security Notes](#security-notes)
- [Future Enhancements](#future-enhancements)
- [License](#license)

## ✨ Features

### Core Functionality
- **User Authentication**: Register and login with PBKDF2-HMAC-SHA256 hashed passwords (100k iterations)
- **Text Secret Management**: Create, read, update, delete encrypted text secrets with AES-256-CBC
- **File Encryption**: Upload files (up to 10 MB) with AES-256 encryption and in-app preview for images/text
- **Search & Filter**: Search secrets by key name with real-time filtering
- **Audit Trail**: Comprehensive access logging for all operations (VIEW, ADD, UPDATE, DELETE, DOWNLOAD_FILE)

### Security Features
- **AES-256-CBC Encryption**: Industry-standard symmetric encryption with random IVs per operation
- **PBKDF2 Key Derivation**: 100,000 iterations with SHA-256 for master password → encryption key
- **Salted Password Hashing**: Stored as "salt:hash" format to prevent rainbow table attacks
- **Decrypt-on-Demand**: Plaintext only exists in memory during view operations
- **Ciphertext Verification**: "View Cipher" and "File Cipher" buttons display Base64-encoded ciphertext for proof

### User Experience
- **Clean Swing GUI**: Professional desktop interface with dialogs and table views
- **File Preview**: In-app preview for images (PNG/JPG/GIF) and text files (TXT/CSV/LOG)
- **Responsive Design**: Proper form validation, error messages, and success confirmations
- **Multi-user Support**: Each user has isolated secrets and files with foreign key constraints

## 🛠 Technology Stack

- **Language**: Java 17+
- **GUI Framework**: Java Swing
- **Database**: MySQL 8.0+
- **JDBC Driver**: MySQL Connector/J 8.0+
- **Encryption**: Java Cryptography Extension (JCE) - AES-256-CBC, PBKDF2-HMAC-SHA256
- **Build Tool**: IntelliJ IDEA / Manual compilation

## 🔒 Security Architecture

### Password Storage
- **Hashing Algorithm**: PBKDF2-HMAC-SHA256
- **Iterations**: 100,000
- **Salt**: 16-byte random salt per user
- **Format**: Stored as "salt:hash" (Base64-encoded) in VARCHAR(512)

### Data Encryption
- **Algorithm**: AES-256-CBC
- **Key Derivation**: PBKDF2-HMAC-SHA256 from master password
- **IV**: 16-byte random IV generated per encryption operation
- **Storage Format**: Base64(IV || CIPHERTEXT) for text secrets; raw bytes for files

### Data Flow
1. User enters master password → PBKDF2 derives 256-bit key
2. Plaintext secret → AES-256-CBC with random IV → Base64 ciphertext → DB
3. File upload → AES-256-CBC → LONGBLOB storage (IV prepended to ciphertext)
4. Retrieval → Decrypt with user's master password-derived key → Display/Download

## 📦 Prerequisites

- **Java Development Kit (JDK)**: 17 or higher
- **MySQL Server**: 8.0 or higher
- **MySQL Connector/J**: 8.0+ (JDBC driver)
- **IDE** (optional): IntelliJ IDEA, Eclipse, or NetBeans

## 🚀 Installation & Setup

### 1. Clone the Repository
# Secure-Data-Vault
