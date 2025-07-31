# 🔨 Build & Deploy Guide

## Overview

The **Build & Deploy** script (`build-deploy.sh`) builds new Docker images from source code and deploys them. This is perfect for:

- **Development workflows** when making code changes
- **Custom modifications** to the application
- **Creating new releases** with specific features
- **Testing changes** before production deployment

## 📋 Prerequisites

Before using the build & deploy script, ensure you have:

### Required Tools
- **kubectl** - Kubernetes command-line tool
- **Docker** - For building and pushing images
- **Maven** - For building Java backend (3.6+)
- **Node.js & npm** - For building React frontend (16+)
- **Git** - For version tracking (optional)

### Development Environment
- **Java 17+** - For backend compilation
- **Docker BuildX** - For multi-platform builds
- **Registry access** - Push permissions to your Docker registry

### Cluster Requirements
- **Kubernetes 1.19+** recommended
- **Ingress controller** installed
- **Storage class** available for PostgreSQL
- **DNS configuration** for your hostname

## 🎯 Use Cases

### 1. **Development Workflow**
Build and test your changes:
```bash
# Make your code changes
vim backend/src/main/java/...
vim frontend/src/...

# Build and deploy
export REGISTRY="your-dockerhub-username"
export HOSTNAME="kafka-dev.yourcompany.com"
export NAMESPACE="kafka-dev"
export BUILD_TAG="dev-$(date +%Y%m%d)"
./build-deploy.sh
```

### 2. **Feature Development**
Create a feature-specific deployment:
```bash
export BUILD_TAG="feature-message-search"
export NAMESPACE="kafka-feature-test"
export HOSTNAME="kafka-feature.yourcompany.com"
./build-deploy.sh
```

### 3. **Release Creation**
Build and deploy a new release:
```bash
export BUILD_TAG="v2.1.0"
export NAMESPACE="kafka-staging"
export HOSTNAME="kafka-staging.yourcompany.com"
./build-deploy.sh
```

## 🚀 Getting Started

### Step 1: Setup Development Environment
```bash
# Clone the repository
git clone https://github.com/your-username/kafka-web-tool.git
cd kafka-web-tool

# Install backend dependencies
cd backend
mvn clean install
cd ..

# Install frontend dependencies
cd frontend
npm install
cd ..

# Make the script executable
chmod +x build-deploy.sh
```

### Step 2: Configure Docker Registry
```bash
# Login to your Docker registry
docker login

# For DockerHub
docker login -u your-username

# For Google Container Registry
gcloud auth configure-docker

# For AWS ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-west-2.amazonaws.com
```

### Step 3: Build and Deploy
```bash
# Interactive build and deployment
./build-deploy.sh

# The script will prompt you for:
# - Docker registry
# - Build tag
# - Application hostname
# - Kubernetes namespace
```

## ⚙️ Configuration Options

### Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `REGISTRY` | Docker registry | `your-registry` | `your-dockerhub-username` |
| `BUILD_TAG` | Docker image tag | `latest` | `v2.1.0`, `dev-20240131` |
| `HOSTNAME` | Application hostname | `your-hostname.com` | `kafka-dev.example.com` |
| `NAMESPACE` | Kubernetes namespace | `kafka-tool` | `kafka-development` |
| `BACKEND_IMAGE` | Backend image name | `kafka-web-app-v2` | `my-kafka-backend` |
| `FRONTEND_IMAGE` | Frontend image name | `kafka-web-app-frontend` | `my-kafka-frontend` |

### Command Options

```bash
# Build images only (no deployment)
./build-deploy.sh build

# Full build and deployment (default)
./build-deploy.sh deploy

# Check deployment status
./build-deploy.sh status

# Clean up deployment
./build-deploy.sh clean
```

## 🔧 Build Process Details

### Backend Build Process
1. **Maven Compilation** - Compiles Java source code
2. **Dependency Resolution** - Downloads required libraries
3. **JAR Creation** - Packages application into executable JAR
4. **Docker Build** - Creates multi-platform Docker image
5. **Registry Push** - Uploads image to Docker registry

### Frontend Build Process
1. **Dependency Installation** - Downloads npm packages
2. **React Build** - Compiles TypeScript and bundles assets
3. **Docker Build** - Creates optimized production image
4. **Registry Push** - Uploads image to Docker registry

### Build Optimization
The script uses Docker BuildX for:
- **Multi-platform builds** (AMD64 for Linux clusters)
- **Build caching** for faster subsequent builds
- **Parallel builds** when possible

## 📊 What Gets Built and Deployed

### Images Created
- **Backend Image**: `{REGISTRY}/{BACKEND_IMAGE}:{BUILD_TAG}`
- **Frontend Image**: `{REGISTRY}/{FRONTEND_IMAGE}:{BUILD_TAG}`
- **Versioned Tags**: Additional tags with timestamp and git commit

### Deployment Components
- **Infrastructure**: PostgreSQL, Redis, ConfigMaps, Secrets
- **Application**: Backend API, Frontend web app
- **Networking**: Services, Ingress with custom hostname
- **Configuration**: Auto-configured for new namespace

### 🔐 **Default Login Credentials**
Your deployed application will have a login page. Use these default credentials:

- **Username**: `admin`
- **Password**: `admin123`

> ⚠️ **Security Note**: Change these credentials in production by updating the application configuration.

## 🔄 Development Workflows

### Feature Development
```bash
# Create feature branch
git checkout -b feature/new-awesome-feature

# Make your changes
# ... edit code ...

# Build and test
export BUILD_TAG="feature-awesome"
export NAMESPACE="kafka-feature-test"
export HOSTNAME="kafka-feature.dev.example.com"
./build-deploy.sh

# Test your changes at https://kafka-feature.dev.example.com
```

### Bug Fixes
```bash
# Create bugfix branch
git checkout -b bugfix/fix-message-display

# Fix the issue
# ... edit code ...

# Build and test
export BUILD_TAG="bugfix-$(date +%Y%m%d)"
export NAMESPACE="kafka-bugfix-test"
./build-deploy.sh
```

### Release Preparation
```bash
# Create release branch
git checkout -b release/v2.1.0

# Build release candidate
export BUILD_TAG="v2.1.0-rc1"
export NAMESPACE="kafka-staging"
export HOSTNAME="kafka-staging.example.com"
./build-deploy.sh

# After testing, create final release
export BUILD_TAG="v2.1.0"
./build-deploy.sh
```

## 🧪 Testing Your Changes

### Local Testing
```bash
# Build images only
./build-deploy.sh build

# Test locally with Docker Compose
docker-compose up -d

# Access at http://localhost:3000
```

### Kubernetes Testing
```bash
# Deploy to test namespace
export NAMESPACE="kafka-test-$(whoami)"
export HOSTNAME="kafka-test-$(whoami).dev.example.com"
./build-deploy.sh

# Run tests
kubectl port-forward service/kafka-web-app-frontend 3000:80 -n kafka-test-$(whoami)
```

## 🔍 Monitoring Build Process

### Build Logs
The script provides detailed logging:
- **Blue [INFO]** - General information
- **Cyan [BUILD]** - Build-specific steps
- **Purple [STEP]** - Major deployment steps
- **Green [SUCCESS]** - Successful operations
- **Yellow [WARNING]** - Warnings
- **Red [ERROR]** - Errors

### Build Artifacts
```bash
# Check built images
docker images | grep kafka-web-app

# Check image details
docker inspect your-registry/kafka-web-app-v2:your-tag

# Check registry
docker search your-registry/kafka-web-app-v2
```

## 🔧 Customization

### Build Customization
Edit the build process by modifying:

#### Backend Build
```bash
# Custom Maven profiles
cd backend
mvn clean package -P production -DskipTests

# Custom JVM options
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
```

#### Frontend Build
```bash
# Custom build environment
cd frontend
export NODE_ENV=production
export REACT_APP_API_URL=https://your-api.example.com
npm run build
```

### Dockerfile Customization
Modify the Dockerfiles for custom requirements:

```dockerfile
# backend/Dockerfile - Add custom tools
RUN apt-get update && apt-get install -y your-tool

# frontend/Dockerfile - Custom nginx config
COPY custom-nginx.conf /etc/nginx/nginx.conf
```

## 🚨 Troubleshooting

### Common Build Issues

#### 1. **Maven Build Failures**
```bash
# Check Java version
java -version

# Clean Maven cache
mvn clean install -U

# Skip tests if needed
mvn clean package -DskipTests
```

#### 2. **npm Build Failures**
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Use Docker build instead
docker build -t test-frontend frontend/
```

#### 3. **Docker Build Issues**
```bash
# Check Docker daemon
docker info

# Clean Docker cache
docker system prune -a

# Check available space
df -h
```

#### 4. **Registry Push Issues**
```bash
# Check authentication
docker login your-registry

# Check permissions
docker push your-registry/test-image:latest

# Check network connectivity
ping your-registry
```

### Performance Optimization

#### Build Speed
```bash
# Use build cache
export DOCKER_BUILDKIT=1

# Parallel builds
docker buildx create --use

# Local registry for caching
docker run -d -p 5000:5000 --name registry registry:2
```

#### Resource Usage
```bash
# Limit build resources
docker build --memory=2g --cpus=2 .

# Monitor resource usage
docker stats
```

## 🔒 Security Best Practices

### Image Security
- **Scan images** for vulnerabilities
- **Use minimal base images** (Alpine Linux)
- **Run as non-root user** in containers
- **Keep dependencies updated**

### Registry Security
- **Use private registries** for proprietary code
- **Enable image signing** for production
- **Implement access controls**
- **Regular security audits**

### Build Security
- **Secure build environment**
- **Use secrets management** for credentials
- **Audit build dependencies**
- **Implement build verification**

## 📈 Advanced Features

### Multi-Environment Builds
```bash
# Build for multiple environments
for env in dev staging prod; do
  export BUILD_TAG="v2.1.0-$env"
  export NAMESPACE="kafka-$env"
  export HOSTNAME="kafka-$env.example.com"
  ./build-deploy.sh build
done
```

### Automated Builds
```bash
# CI/CD integration
#!/bin/bash
if [ "$BRANCH" = "main" ]; then
  export BUILD_TAG="latest"
  export NAMESPACE="kafka-production"
elif [ "$BRANCH" = "develop" ]; then
  export BUILD_TAG="develop"
  export NAMESPACE="kafka-staging"
fi
./build-deploy.sh
```

## 🎯 **First Time Access**

After your build and deployment completes:

1. **Access your application** at the configured hostname
2. **Login with default credentials**:
   - Username: `admin`
   - Password: `admin123`
3. **Test your changes**:
   - Verify your modifications work as expected
   - Test the edit/delete message functionality
   - Check any custom features you've added

> 💡 **Development Tip**: Use browser developer tools to debug frontend changes and check the backend API responses.

---

**🔨 Happy Building!** The build & deploy script gives you full control over the build process and enables rapid development and testing of your Kafka Web Tool modifications.
