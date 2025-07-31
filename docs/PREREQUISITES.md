# Prerequisites

This document outlines all the requirements needed to successfully deploy and run the Kafka Web Tool v2.0.

## 🖥️ System Requirements

### Minimum Requirements
- **CPU**: 2 cores
- **Memory**: 4GB RAM
- **Storage**: 10GB available disk space
- **Network**: Internet access for downloading dependencies

### Recommended Requirements
- **CPU**: 4+ cores
- **Memory**: 8GB+ RAM
- **Storage**: 50GB+ available disk space
- **Network**: High-speed internet connection

## 🐳 Container Platform

### Docker
- **Version**: Docker 20.10+ or Docker Desktop 4.0+
- **Docker Compose**: Version 2.0+

**Installation:**
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# macOS
brew install docker docker-compose

# Windows
# Download Docker Desktop from https://www.docker.com/products/docker-desktop
```

### Kubernetes (Production)
- **Version**: Kubernetes 1.24+
- **kubectl**: Latest version
- **Cluster**: Minimum 3 nodes recommended

**Supported Platforms:**
- Amazon EKS
- Google GKE
- Azure AKS
- Self-managed Kubernetes
- Minikube (development only)

## 🗄️ Database

### PostgreSQL
- **Version**: PostgreSQL 15+
- **Storage**: 10GB+ for production
- **Configuration**: UTF-8 encoding

**Required Extensions:**
- `uuid-ossp` (for UUID generation)

**Example Setup:**
```sql
-- Create database
CREATE DATABASE kafka_web_tool;

-- Create user
CREATE USER kafka_user WITH PASSWORD 'secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE kafka_web_tool TO kafka_user;

-- Enable UUID extension
\c kafka_web_tool
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

## ☕ Development Environment (Optional)

### Java Development Kit
- **Version**: OpenJDK 21+ or Oracle JDK 21+
- **JAVA_HOME**: Properly configured

**Installation:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21

# Windows
# Download from https://adoptium.net/
```

### Maven
- **Version**: Maven 3.9+

**Installation:**
```bash
# Ubuntu/Debian
sudo apt install maven

# macOS
brew install maven

# Windows
# Download from https://maven.apache.org/download.cgi
```

### Node.js (Frontend Development)
- **Version**: Node.js 18+
- **Package Manager**: npm 9+ or yarn 1.22+

**Installation:**
```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# macOS
brew install node

# Windows
# Download from https://nodejs.org/
```

## 🔗 Apache Kafka

### Kafka Cluster
- **Version**: Apache Kafka 2.8+ (Kafka 3.x recommended)
- **Connectivity**: Network access from the application
- **Authentication**: SASL/PLAIN, SASL/SCRAM, or mTLS supported

**Supported Kafka Distributions:**
- Apache Kafka
- Confluent Platform
- Amazon MSK
- Azure Event Hubs for Kafka
- Google Cloud Pub/Sub (Kafka API)

### Required Kafka Configurations
```properties
# Minimum broker configuration
num.network.threads=8
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Topic management
auto.create.topics.enable=false
delete.topic.enable=true
```

## 🔒 Security Requirements

### TLS/SSL Certificates (Production)
- **Certificate Authority**: Valid CA-signed certificates
- **Key Format**: PEM format
- **Cipher Suites**: TLS 1.2+ supported

### Network Security
- **Firewall**: Configure appropriate ports
- **Load Balancer**: HTTPS termination capability
- **DNS**: Valid domain name for production

### Required Ports
```
Application Ports:
- 8080: Backend API (HTTP)
- 8443: Backend API (HTTPS)
- 3000: Frontend (HTTP)
- 3443: Frontend (HTTPS)

Database Ports:
- 5432: PostgreSQL

Kafka Ports:
- 9092: Kafka (PLAINTEXT)
- 9093: Kafka (SSL)
- 9094: Kafka (SASL_SSL)
```

## 🌐 Network Requirements

### Internet Access
- **Docker Hub**: For pulling base images
- **Maven Central**: For Java dependencies
- **npm Registry**: For Node.js packages
- **GitHub**: For source code (if applicable)

### Internal Network
- **Database Access**: Application to PostgreSQL
- **Kafka Access**: Application to Kafka brokers
- **Service Discovery**: If using Kubernetes

## 📊 Monitoring (Optional)

### Metrics Collection
- **Prometheus**: For metrics scraping
- **Grafana**: For visualization
- **AlertManager**: For alerting

### Logging
- **Log Aggregation**: ELK Stack, Fluentd, or similar
- **Log Retention**: Configure appropriate retention policies

## 🔧 Operating System Support

### Supported Platforms
- **Linux**: Ubuntu 20.04+, CentOS 8+, RHEL 8+
- **macOS**: macOS 11+ (development)
- **Windows**: Windows 10+ with WSL2 (development)

### Container Orchestration
- **Kubernetes**: 1.24+
- **Docker Swarm**: 20.10+
- **OpenShift**: 4.10+

## ✅ Pre-deployment Checklist

### Infrastructure
- [ ] Kubernetes cluster is running and accessible
- [ ] kubectl is configured and working
- [ ] PostgreSQL database is available
- [ ] Kafka cluster is accessible
- [ ] Required ports are open
- [ ] DNS resolution is working

### Security
- [ ] TLS certificates are available (production)
- [ ] Database credentials are secure
- [ ] Kafka authentication is configured
- [ ] Network policies are in place
- [ ] Firewall rules are configured

### Resources
- [ ] Sufficient CPU and memory allocated
- [ ] Storage volumes are provisioned
- [ ] Backup strategy is in place
- [ ] Monitoring is configured

### Access
- [ ] Container registry access (if using private registry)
- [ ] Database connection tested
- [ ] Kafka connection tested
- [ ] Load balancer configured (production)

## 🆘 Troubleshooting Prerequisites

### Common Issues

**Docker Issues:**
```bash
# Check Docker status
docker version
docker-compose version

# Test Docker connectivity
docker run hello-world
```

**Kubernetes Issues:**
```bash
# Check cluster status
kubectl cluster-info
kubectl get nodes

# Check resource availability
kubectl top nodes
kubectl top pods
```

**Database Issues:**
```bash
# Test PostgreSQL connection
psql -h localhost -U kafka_user -d kafka_web_tool

# Check database version
SELECT version();
```

**Kafka Issues:**
```bash
# Test Kafka connectivity
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check Kafka version
kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

## 📞 Support

If you encounter issues with prerequisites:

1. **Check Logs**: Review system and application logs
2. **Verify Versions**: Ensure all components meet minimum version requirements
3. **Test Connectivity**: Verify network access between components
4. **Review Documentation**: Check component-specific documentation
5. **Contact Support**: Reach out with specific error messages and environment details

---

**Next Step:** Once all prerequisites are met, proceed to the [Deployment Guide](DEPLOYMENT.md).
