# 🚀 Kafka Web Tool - Deployment Guide

## 📋 Deployment Configuration

**Environment Details:**
- **Kubernetes Cluster**: Ubuntu-based cluster
- **DockerHub Repository**: `rmqk8`
- **Namespace**: `kafka-tool`
- **Hostname**: `kafkawebtool.marsem.org`
- **Branch**: `feature/message-edit-delete`

## 🎯 New Feature: Edit/Delete Messages

This deployment includes the new **message edit and delete functionality** with:
- ✅ Edit messages (sends updated version with same key)
- ✅ Delete messages (sends tombstone messages)
- ✅ Enhanced UI with loading states and error handling
- ✅ Smart validation (only shows for messages with keys)

## 🛠️ Deployment Scripts

### 1. Full Application Deployment
```bash
# Deploy the complete application (first time or full redeploy)
./deploy.sh
```

### 2. Feature-Specific Deployment
```bash
# Deploy just the new edit/delete message feature
./deploy-feature.sh
```

### 3. Build Only
```bash
# Build and push images without deploying
./deploy-feature.sh build
```

### 4. Verify Deployment
```bash
# Check deployment status
./deploy-feature.sh verify
```

### 5. Rollback
```bash
# Rollback to previous version if needed
./deploy-feature.sh rollback
```

## 📦 Docker Images

The deployment creates and pushes these images to DockerHub:

- **Backend**: `rmqk8/kafka-web-app-v2:latest`
- **Frontend**: `rmqk8/kafka-web-app-frontend:latest`

Images are tagged with:
- `latest` - Current stable version
- `edit-delete-messages-YYYYMMDD-HHMMSS-{git-commit}` - Feature-specific tag

## 🔧 Prerequisites

Before deployment, ensure you have:

1. **Docker** installed and logged into DockerHub
2. **kubectl** configured for your Kubernetes cluster
3. **Maven** for building the Java backend
4. **Node.js/npm** for building the React frontend
5. **Access** to the `kafka-tool` namespace

## 🚀 Quick Deployment

For the new edit/delete message feature:

```bash
# 1. Ensure you're on the feature branch
git checkout feature/message-edit-delete

# 2. Deploy the feature
./deploy-feature.sh

# 3. Verify deployment
./deploy-feature.sh verify
```

## 🌐 Access URLs

After deployment, access the application at:

- **Frontend**: https://kafkawebtool.marsem.org
- **Backend API**: https://kafkawebtool.marsem.org/api/v1
- **Health Check**: https://kafkawebtool.marsem.org/api/v1/health
- **API Docs**: https://kafkawebtool.marsem.org/api/v1/swagger-ui.html

## 🧪 Testing the New Feature

1. **Navigate** to the Consumer or Message Browser pages
2. **Look for** Edit (✏️) and Delete (🗑️) buttons next to messages
3. **Note**: Buttons only appear for messages that have keys
4. **Test Edit**: Click edit button, modify content, save
5. **Test Delete**: Click delete button, confirm tombstone sending

## 📊 Monitoring

Check deployment status:

```bash
# Pod status
kubectl get pods -n kafka-tool

# Service status
kubectl get services -n kafka-tool

# Ingress status
kubectl get ingress -n kafka-tool

# Logs
kubectl logs -f deployment/kafka-web-app-backend -n kafka-tool
kubectl logs -f deployment/kafka-web-app-frontend -n kafka-tool
```

## 🔄 Rolling Updates

The deployment uses rolling updates with:
- **Zero downtime** deployment
- **Health checks** to ensure pod readiness
- **Automatic rollback** on failure

## 🛡️ Security

- **Non-root containers** for security
- **Resource limits** to prevent resource exhaustion
- **Health checks** for reliability
- **TLS termination** at ingress level

## 📝 Troubleshooting

### Common Issues:

1. **Image Pull Errors**
   ```bash
   # Check if images exist in DockerHub
   docker pull rmqk8/kafka-web-app-v2:latest
   docker pull rmqk8/kafka-web-app-frontend:latest
   ```

2. **Pod Not Ready**
   ```bash
   # Check pod logs
   kubectl describe pod <pod-name> -n kafka-tool
   kubectl logs <pod-name> -n kafka-tool
   ```

3. **Ingress Issues**
   ```bash
   # Check ingress configuration
   kubectl describe ingress kafka-web-app-ingress -n kafka-tool
   ```

4. **Database Connection**
   ```bash
   # Check if PostgreSQL is running
   kubectl get pods -n kafka-tool -l app=postgres
   ```

## 🔧 Configuration

Key configuration files:
- `k8s/deployment.yaml` - Main application deployment
- `k8s/ingress.yaml` - Ingress configuration
- `k8s/configmap.yaml` - Application configuration
- `k8s/secret.yaml` - Sensitive configuration

## 📞 Support

For deployment issues:
1. Check the logs using kubectl commands above
2. Verify all prerequisites are met
3. Ensure the Kubernetes cluster is accessible
4. Check DockerHub for image availability

---

**🎉 Happy Deploying!** The new edit/delete message feature is ready for production use.
