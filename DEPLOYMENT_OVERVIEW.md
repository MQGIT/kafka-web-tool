# 🚀 Kafka Web Tool - Deployment Overview

## Two Deployment Approaches

The Kafka Web Tool provides **two distinct deployment scripts** designed for different use cases and workflows:

### 🏃‍♂️ **Quick Deploy** (`quick-deploy.sh`)
**Uses pre-built images for fast deployment**

- ✅ **Fast deployment** (2-5 minutes)
- ✅ **No build dependencies** required
- ✅ **Production ready** with stable images
- ✅ **Perfect for demos** and testing
- ✅ **Minimal prerequisites**

### 🔨 **Build & Deploy** (`build-deploy.sh`)
**Builds new images from source code**

- ✅ **Full control** over build process
- ✅ **Custom modifications** supported
- ✅ **Development workflow** optimized
- ✅ **Version control** integration
- ✅ **Feature development** friendly

---

## 🎯 When to Use Which Script

### Use **Quick Deploy** when:
- 🎪 **Demonstrating** the application to stakeholders
- 🏭 **Production deployment** with tested, stable images
- ⚡ **Quick testing** of the application features
- 🎓 **Learning** and exploring the tool
- 🔄 **Deploying to multiple environments** with same codebase
- 📦 **Using official releases** without modifications

### Use **Build & Deploy** when:
- 🛠️ **Developing new features** or fixing bugs
- 🎨 **Customizing** the application for your needs
- 🧪 **Testing code changes** before release
- 📋 **Creating custom releases** with specific features
- 🔧 **Modifying configuration** or adding integrations
- 🏗️ **Contributing** to the project development

---

## 📊 Comparison Matrix

| Feature | Quick Deploy | Build & Deploy |
|---------|--------------|----------------|
| **Deployment Time** | 2-5 minutes | 10-20 minutes |
| **Prerequisites** | kubectl, Docker (optional) | kubectl, Docker, Maven, Node.js |
| **Build Process** | ❌ None | ✅ Full build from source |
| **Customization** | ⚠️ Limited to config | ✅ Full source code control |
| **Use Case** | Production, Demo, Testing | Development, Custom builds |
| **Image Source** | Pre-built registry images | Built from local source |
| **Version Control** | Uses tagged releases | Uses current git state |
| **Dependencies** | Minimal | Full development stack |
| **Disk Space** | Low | High (build artifacts) |
| **Network Usage** | Image download only | Image upload required |

---

## 🚀 Quick Start Guide

### Option 1: Quick Deploy (Recommended for first-time users)

```bash
# 1. Clone the repository
git clone https://github.com/your-username/kafka-web-tool.git
cd kafka-web-tool

# 2. Set your configuration
export HOSTNAME="kafka-tool.your-domain.com"
export NAMESPACE="kafka-tool"

# 3. Deploy in minutes
./quick-deploy.sh
```

### Option 2: Build & Deploy (For developers)

```bash
# 1. Clone and setup development environment
git clone https://github.com/your-username/kafka-web-tool.git
cd kafka-web-tool

# 2. Install development dependencies
# Java 17+, Maven 3.6+, Node.js 16+, Docker

# 3. Configure your registry
export REGISTRY="your-dockerhub-username"
export HOSTNAME="kafka-dev.your-domain.com"
export BUILD_TAG="dev-$(date +%Y%m%d)"

# 4. Build and deploy
./build-deploy.sh
```

---

## 🔧 Configuration Options

Both scripts support the same configuration options but handle them differently:

### Environment Variables

| Variable | Description | Quick Deploy Default | Build & Deploy Default |
|----------|-------------|---------------------|------------------------|
| `REGISTRY` | Docker registry | `rmqk8` (official) | `your-registry` (custom) |
| `HOSTNAME` | Application hostname | Prompted | Prompted |
| `NAMESPACE` | Kubernetes namespace | `kafka-tool` | `kafka-tool` |
| `BUILD_TAG` | Image tag | `latest` | `latest` |

### Interactive vs Automated

```bash
# Interactive mode (both scripts)
./quick-deploy.sh
./build-deploy.sh

# Automated mode with environment variables
export REGISTRY="your-registry"
export HOSTNAME="kafka.example.com"
export NAMESPACE="kafka-production"

./quick-deploy.sh    # Uses pre-built images
./build-deploy.sh    # Builds new images
```

---

## 🏗️ Architecture Deployed

Both scripts deploy the same architecture:

### Infrastructure Layer
- **PostgreSQL** - Database for connection storage
- **Redis** - Caching and session management
- **ConfigMaps** - Application configuration
- **Secrets** - Sensitive data management

### Application Layer
- **Backend** - Java Spring Boot API (3 replicas)
  - Edit/Delete message endpoints
  - Topic management
  - Connection management
  - Real-time message streaming
- **Frontend** - React web application (3 replicas)
  - Message browser with edit/delete UI
  - Consumer management interface
  - Producer interface
  - Real-time updates

### Network Layer
- **Services** - Internal load balancing
- **Ingress** - External access with TLS
- **DNS** - Custom hostname configuration

---

## 🎯 Feature Highlights

Both deployment methods provide the complete feature set:

### 📨 **Message Management**
- **Browse Messages** - Real-time message viewing with pagination
- **Edit Messages** - Modify message content (sends updated version)
- **Delete Messages** - Send tombstone messages for logical deletion
- **Produce Messages** - Send new messages with custom headers

### 🎛️ **Topic Management**
- **Create Topics** - Configure partitions and replication
- **Delete Topics** - Remove topics safely
- **Topic Statistics** - View partition and offset information

### 🔗 **Connection Management**
- **Multiple Clusters** - Connect to different Kafka environments
- **Secure Connections** - SSL/SASL authentication support
- **Connection Testing** - Validate connectivity before saving

### 🎨 **User Interface**
- **Modern Design** - React-based responsive interface
- **Real-time Updates** - WebSocket-based live data
- **Error Handling** - Comprehensive user feedback
- **Loading States** - Visual feedback during operations

---

## 📚 Documentation Structure

### Quick Deploy Documentation
- **[QUICK_DEPLOY_GUIDE.md](QUICK_DEPLOY_GUIDE.md)** - Comprehensive guide for quick deployment
- **Use Cases** - Production, demo, testing scenarios
- **Configuration** - Environment variables and options
- **Troubleshooting** - Common issues and solutions

### Build & Deploy Documentation
- **[BUILD_DEPLOY_GUIDE.md](BUILD_DEPLOY_GUIDE.md)** - Complete development workflow guide
- **Development Setup** - Prerequisites and environment
- **Build Process** - Detailed build steps and customization
- **Advanced Features** - Multi-environment builds, CI/CD integration

---

## 🔄 Workflow Examples

### Development Workflow
```bash
# 1. Make code changes
vim backend/src/main/java/...
vim frontend/src/components/...

# 2. Build and test
export BUILD_TAG="feature-$(date +%Y%m%d)"
export NAMESPACE="kafka-dev-$(whoami)"
./build-deploy.sh

# 3. Test changes
curl https://kafka-dev-$(whoami).example.com/api/v1/health

# 4. Deploy to staging
export BUILD_TAG="staging-v2.1.0"
export NAMESPACE="kafka-staging"
./build-deploy.sh
```

### Production Deployment
```bash
# 1. Use tested images
export REGISTRY="rmqk8"
export HOSTNAME="kafka.yourcompany.com"
export NAMESPACE="kafka-production"

# 2. Quick production deployment
./quick-deploy.sh

# 3. Verify deployment
./quick-deploy.sh status
```

---

## 🆘 Getting Help

### Documentation
- **README.md** - Main project documentation
- **QUICK_DEPLOY_GUIDE.md** - Quick deployment guide
- **BUILD_DEPLOY_GUIDE.md** - Build and deployment guide
- **API Documentation** - Available at `/api/v1/swagger-ui.html`

### Support Channels
- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - Questions and community support
- **Documentation Wiki** - Extended guides and tutorials

### Useful Commands
```bash
# Check deployment status
kubectl get all -n your-namespace

# View application logs
kubectl logs -f deployment/kafka-web-app-backend -n your-namespace

# Access application locally
kubectl port-forward service/kafka-web-app-frontend 3000:80 -n your-namespace
```

---

## 🎉 Ready to Deploy!

Choose your deployment approach based on your needs:

- **🏃‍♂️ New to Kafka Web Tool?** → Start with **Quick Deploy**
- **🔨 Want to customize or develop?** → Use **Build & Deploy**
- **🏭 Production deployment?** → Use **Quick Deploy** with official images
- **🧪 Testing changes?** → Use **Build & Deploy** with your modifications

Both scripts are designed to be user-friendly, well-documented, and production-ready. Happy deploying! 🚀
