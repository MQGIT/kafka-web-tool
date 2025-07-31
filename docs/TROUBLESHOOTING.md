# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the Kafka Web Tool v2.0.

## 🚨 Common Issues

### 1. Application Won't Start

#### Backend Not Starting
**Symptoms:**
- Backend pods in `CrashLoopBackOff` state
- Error logs showing database connection issues
- Health check endpoints returning 503

**Diagnosis:**
```bash
# Check pod status
kubectl get pods -n kafka-tool

# Check pod logs
kubectl logs deployment/kafka-web-app-backend -n kafka-tool

# Check events
kubectl get events -n kafka-tool --sort-by='.lastTimestamp'
```

**Common Causes & Solutions:**

**Database Connection Issues:**
```bash
# Check database connectivity
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- nc -zv postgres 5432

# Verify database credentials
kubectl get secret postgres-secret -n kafka-tool -o yaml

# Check database logs
kubectl logs deployment/postgres -n kafka-tool
```

**Memory/Resource Issues:**
```bash
# Check resource usage
kubectl top pods -n kafka-tool

# Increase memory limits
kubectl patch deployment kafka-web-app-backend -n kafka-tool -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend","resources":{"limits":{"memory":"2Gi"}}}]}}}}'
```

#### Frontend Not Starting
**Symptoms:**
- Frontend pods failing to start
- Nginx configuration errors
- Static files not loading

**Diagnosis:**
```bash
# Check frontend logs
kubectl logs deployment/kafka-web-app-frontend -n kafka-tool

# Check nginx configuration
kubectl exec -it deployment/kafka-web-app-frontend -n kafka-tool -- nginx -t
```

**Solutions:**
```bash
# Restart frontend deployment
kubectl rollout restart deployment/kafka-web-app-frontend -n kafka-tool

# Check nginx config
kubectl describe configmap nginx-config -n kafka-tool
```

### 2. Database Issues

#### Connection Timeouts
**Symptoms:**
- "Connection timeout" errors in backend logs
- Slow application response times
- Database connection pool exhaustion

**Diagnosis:**
```bash
# Check database connections
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "SELECT count(*) FROM pg_stat_activity;"

# Check connection pool settings
kubectl logs deployment/kafka-web-app-backend -n kafka-tool | grep -i "hikari\|connection"
```

**Solutions:**
```bash
# Increase connection pool size
kubectl set env deployment/kafka-web-app-backend -n kafka-tool DB_MAX_CONNECTIONS=50

# Restart database
kubectl rollout restart deployment/postgres -n kafka-tool
```

#### Database Migration Issues
**Symptoms:**
- "Table doesn't exist" errors
- Schema version conflicts
- Data corruption

**Diagnosis:**
```bash
# Check database schema
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "\dt"

# Check migration logs
kubectl logs deployment/kafka-web-app-backend -n kafka-tool | grep -i "liquibase\|migration\|schema"
```

**Solutions:**
```bash
# Force schema recreation (CAUTION: Data loss)
kubectl set env deployment/kafka-web-app-backend -n kafka-tool SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop

# Manual migration
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -f /path/to/migration.sql
```

### 3. Kafka Connectivity Issues

#### Cannot Connect to Kafka
**Symptoms:**
- "Connection refused" errors when testing Kafka connections
- Timeout errors when listing topics
- Authentication failures

**Diagnosis:**
```bash
# Test Kafka connectivity from backend pod
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- nc -zv kafka-broker 9092

# Check Kafka logs
kubectl logs deployment/kafka-web-app-backend -n kafka-tool | grep -i kafka

# Test with kafka-console-consumer
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- kafka-console-consumer.sh --bootstrap-server kafka-broker:9092 --topic test --from-beginning --max-messages 1
```

**Solutions:**

**Network Issues:**
```bash
# Check DNS resolution
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- nslookup kafka-broker

# Check firewall rules
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- telnet kafka-broker 9092
```

**Authentication Issues:**
```bash
# Verify Kafka credentials
kubectl get secret kafka-credentials -n kafka-tool -o yaml

# Test SASL authentication
kubectl exec -it deployment/kafka-web-app-backend -n kafka-tool -- kafka-console-consumer.sh --bootstrap-server kafka-broker:9092 --topic test --consumer.config /path/to/client.properties
```

### 4. Performance Issues

#### Slow Response Times
**Symptoms:**
- API responses taking > 5 seconds
- Frontend loading slowly
- High CPU/memory usage

**Diagnosis:**
```bash
# Check resource usage
kubectl top pods -n kafka-tool

# Check application metrics
curl -k https://kafkawebtool.yourdomain.com/api/v1/actuator/metrics

# Check database performance
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

**Solutions:**

**Scale Up Resources:**
```bash
# Increase backend replicas
kubectl scale deployment kafka-web-app-backend --replicas=5 -n kafka-tool

# Increase resource limits
kubectl patch deployment kafka-web-app-backend -n kafka-tool -p '{"spec":{"template":{"spec":{"containers":[{"name":"backend","resources":{"limits":{"memory":"2Gi","cpu":"1000m"}}}]}}}}'
```

**Database Optimization:**
```bash
# Add database indexes
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "CREATE INDEX CONCURRENTLY idx_consumer_sessions_status ON consumer_sessions(status);"

# Analyze query performance
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "EXPLAIN ANALYZE SELECT * FROM consumer_sessions WHERE status = 'RUNNING';"
```

### 5. Consumer Session Issues

#### Stuck Consumer Sessions
**Symptoms:**
- Consumer sessions showing as RUNNING but not consuming messages
- High number of active sessions
- Consumer timeout errors

**Diagnosis:**
```bash
# Check running consumers via API
curl -k "https://kafkawebtool.yourdomain.com/api/v1/dashboard/running-consumers"

# Check consumer logs
kubectl logs deployment/kafka-web-app-backend -n kafka-tool | grep -i "consumer\|session"

# Check database for stuck sessions
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "SELECT session_id, status, created_at, topic FROM consumer_sessions WHERE status = 'RUNNING' AND created_at < NOW() - INTERVAL '5 minutes';"
```

**Solutions:**
```bash
# Stop all running consumers via API
curl -k -X POST "https://kafkawebtool.yourdomain.com/api/v1/dashboard/running-consumers/stop-all"

# Manual cleanup of stuck sessions
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "UPDATE consumer_sessions SET status = 'STOPPED' WHERE status = 'RUNNING' AND created_at < NOW() - INTERVAL '10 minutes';"

# Restart backend to clear in-memory sessions
kubectl rollout restart deployment/kafka-web-app-backend -n kafka-tool
```

### 6. SSL/TLS Issues

#### Certificate Problems
**Symptoms:**
- "Certificate verification failed" errors
- Browser showing "Not secure" warnings
- SSL handshake failures

**Diagnosis:**
```bash
# Check certificate status
kubectl get certificate -n kafka-tool

# Check certificate details
kubectl describe certificate kafka-web-tool-tls -n kafka-tool

# Test SSL connection
openssl s_client -connect kafkawebtool.yourdomain.com:443 -servername kafkawebtool.yourdomain.com
```

**Solutions:**
```bash
# Renew certificate
kubectl delete certificate kafka-web-tool-tls -n kafka-tool
kubectl apply -f k8s/certificate.yaml

# Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager

# Manual certificate creation
kubectl create secret tls kafka-web-tool-tls --cert=path/to/tls.crt --key=path/to/tls.key -n kafka-tool
```

### 7. Ingress Issues

#### 404 Errors
**Symptoms:**
- API endpoints returning 404
- Frontend routes not working
- Ingress not routing correctly

**Diagnosis:**
```bash
# Check ingress status
kubectl get ingress -n kafka-tool

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Test backend service directly
kubectl port-forward service/kafka-web-app-backend 8080:8080 -n kafka-tool
curl http://localhost:8080/api/v1/actuator/health
```

**Solutions:**
```bash
# Update ingress configuration
kubectl apply -f k8s/ingress.yaml

# Restart ingress controller
kubectl rollout restart deployment/ingress-nginx-controller -n ingress-nginx

# Check service endpoints
kubectl get endpoints -n kafka-tool
```

## 🔧 Diagnostic Commands

### Health Checks
```bash
# Overall cluster health
kubectl get nodes
kubectl get pods --all-namespaces | grep -v Running

# Application health
curl -k https://kafkawebtool.yourdomain.com/api/v1/actuator/health
curl -k https://kafkawebtool.yourdomain.com/api/v1/dashboard/health

# Database health
kubectl exec -it deployment/postgres -n kafka-tool -- pg_isready -U postgres
```

### Log Collection
```bash
# Collect all logs
kubectl logs deployment/kafka-web-app-backend -n kafka-tool > backend.log
kubectl logs deployment/kafka-web-app-frontend -n kafka-tool > frontend.log
kubectl logs deployment/postgres -n kafka-tool > postgres.log

# Collect events
kubectl get events -n kafka-tool --sort-by='.lastTimestamp' > events.log

# Collect resource usage
kubectl top pods -n kafka-tool > resource-usage.log
```

### Performance Monitoring
```bash
# Monitor resource usage
watch kubectl top pods -n kafka-tool

# Monitor application metrics
curl -k https://kafkawebtool.yourdomain.com/api/v1/actuator/metrics/jvm.memory.used

# Monitor database connections
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';"
```

## 🆘 Emergency Procedures

### Complete System Recovery
```bash
# 1. Stop all services
kubectl scale deployment kafka-web-app-backend --replicas=0 -n kafka-tool
kubectl scale deployment kafka-web-app-frontend --replicas=0 -n kafka-tool

# 2. Backup database
kubectl exec -it deployment/postgres -n kafka-tool -- pg_dump -U postgres kafka_web_tool > backup.sql

# 3. Restart database
kubectl rollout restart deployment/postgres -n kafka-tool

# 4. Restore services
kubectl scale deployment kafka-web-app-backend --replicas=3 -n kafka-tool
kubectl scale deployment kafka-web-app-frontend --replicas=3 -n kafka-tool
```

### Data Recovery
```bash
# Restore from backup
kubectl exec -i deployment/postgres -n kafka-tool -- psql -U postgres kafka_web_tool < backup.sql

# Reset consumer sessions
kubectl exec -it deployment/postgres -n kafka-tool -- psql -U postgres -d kafka_web_tool -c "UPDATE consumer_sessions SET status = 'STOPPED' WHERE status IN ('RUNNING', 'PAUSED');"
```

## 📞 Getting Help

### Information to Collect
When reporting issues, please provide:

1. **Environment Information:**
   - Kubernetes version: `kubectl version`
   - Application version/tag
   - Environment (development/staging/production)

2. **Error Details:**
   - Exact error messages
   - Steps to reproduce
   - Expected vs actual behavior

3. **System State:**
   - Pod status: `kubectl get pods -n kafka-tool`
   - Resource usage: `kubectl top pods -n kafka-tool`
   - Recent events: `kubectl get events -n kafka-tool`

4. **Logs:**
   - Application logs (last 100 lines)
   - Database logs (if relevant)
   - Ingress controller logs (if relevant)

### Support Channels
- **Documentation**: Check all documentation files
- **GitHub Issues**: Create detailed issue reports
- **Community Forum**: Ask questions and share solutions
- **Enterprise Support**: Priority support for enterprise customers

---

**Remember**: Always backup your data before making significant changes, especially in production environments.
