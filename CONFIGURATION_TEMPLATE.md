# 🔧 Configuration Template

## Required Configuration Changes

Before deploying the Kafka Web Tool, you need to customize these configuration values for your environment:

### 🚨 **REQUIRED: Update These Values**

#### 1. **Docker Registry**
Replace `your-registry` with your actual Docker registry:

**Files to update:**
- `quick-deploy.sh` (line 14)
- `build-deploy.sh` (line 17)
- `deploy.sh` (line 13)

**Examples:**
```bash
# DockerHub
REGISTRY="your-dockerhub-username"

# Google Container Registry
REGISTRY="gcr.io/your-project-id"

# AWS ECR
REGISTRY="123456789012.dkr.ecr.us-west-2.amazonaws.com"

# Azure Container Registry
REGISTRY="yourregistry.azurecr.io"
```

#### 2. **Application Hostname**
Replace `your-hostname.com` with your actual domain:

**Files to update:**
- `k8s/ingress.yaml` (lines 16, 47)
- `k8s/nginx-config.yaml` (line 17)
- `k8s/configmap.yaml` (line 69)
- `backend/src/main/resources/application.yml` (lines 51, 97)
- `backend/src/main/java/org/marsem/kafka/controller/HealthController.java` (line 31)

**Example:**
```yaml
# Replace this
host: your-hostname.com

# With your actual domain
host: kafka-tool.yourcompany.com
```

### 🔧 **Environment-Specific Configurations**

#### 3. **CORS Origins**
Update CORS allowed origins in these files:
- `k8s/configmap.yaml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/org/marsem/kafka/controller/HealthController.java`

```yaml
# Update from
allowed-origins: https://your-hostname.com,http://localhost:3000

# To your actual domains
allowed-origins: https://kafka-tool.yourcompany.com,http://localhost:3000
```

#### 4. **TLS Certificate**
Update the TLS configuration in `k8s/ingress.yaml`:

```yaml
tls:
- hosts:
  - your-hostname.com  # Change to your domain
  secretName: kafka-web-app-tls
```

### 📝 **Quick Configuration Script**

Use this script to quickly update all configuration files:

```bash
#!/bin/bash

# Set your configuration
REGISTRY="your-dockerhub-username"
HOSTNAME="kafka-tool.yourcompany.com"

# Update all configuration files
find . -name "*.yaml" -o -name "*.yml" -o -name "*.sh" -o -name "*.java" | \
  xargs sed -i.bak \
    -e "s/your-registry/$REGISTRY/g" \
    -e "s/your-hostname\.com/$HOSTNAME/g"

echo "Configuration updated!"
echo "Registry: $REGISTRY"
echo "Hostname: $HOSTNAME"
echo ""
echo "Please review the changes and remove .bak files when satisfied."
```

### 🔒 **Security Considerations**

#### 5. **Change Default Credentials**
The application uses default credentials `admin/admin123`. For production:

1. **Update application configuration** to use environment variables
2. **Set secure credentials** via Kubernetes secrets
3. **Enable proper authentication** (OAuth2, LDAP, etc.)

#### 6. **JWT Secret**
Change the default JWT secret in `backend/src/main/resources/application.yml`:

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secure-secret-key-change-in-production}
```

### 🎯 **Deployment-Specific Settings**

#### 7. **Namespace Configuration**
Default namespace is `kafka-tool`. To use a different namespace:

```bash
export NAMESPACE="your-namespace"
```

#### 8. **Resource Limits**
Adjust resource limits in `k8s/deployment.yaml` based on your cluster:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### ✅ **Verification Checklist**

Before deployment, ensure:

- [ ] Docker registry is accessible and you have push permissions
- [ ] Hostname DNS points to your Kubernetes ingress
- [ ] TLS certificate is configured (Let's Encrypt or custom)
- [ ] CORS origins include your hostname
- [ ] Default credentials are changed for production
- [ ] Resource limits are appropriate for your cluster
- [ ] Namespace exists or will be created

### 🚀 **Ready to Deploy**

After updating the configuration:

1. **Quick Deploy** (using pre-built images):
   ```bash
   export REGISTRY="your-dockerhub-username"
   export HOSTNAME="kafka-tool.yourcompany.com"
   ./quick-deploy.sh
   ```

2. **Build & Deploy** (building from source):
   ```bash
   export REGISTRY="your-dockerhub-username"
   export HOSTNAME="kafka-tool.yourcompany.com"
   ./build-deploy.sh
   ```

### 📚 **Additional Resources**

- **[Quick Deploy Guide](QUICK_DEPLOY_GUIDE.md)** - Fast deployment instructions
- **[Build & Deploy Guide](BUILD_DEPLOY_GUIDE.md)** - Development workflow
- **[Deployment Overview](DEPLOYMENT_OVERVIEW.md)** - Choose the right approach

---

**⚠️ Important**: Never commit your actual credentials or environment-specific configurations to version control. Use environment variables or Kubernetes secrets for sensitive data.
