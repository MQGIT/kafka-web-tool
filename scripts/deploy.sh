#!/bin/bash

# Kafka Web Tool v2.0 Deployment Script
# This script automates the deployment process for different environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="development"
NAMESPACE="kafka-tool"
REGISTRY=""
TAG="latest"
SKIP_BUILD=false
SKIP_TESTS=false
DRY_RUN=false

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Kafka Web Tool v2.0 Deployment Script

Usage: $0 [OPTIONS]

Options:
    -e, --environment ENV    Target environment (development|staging|production) [default: development]
    -n, --namespace NS       Kubernetes namespace [default: kafka-tool]
    -r, --registry REG       Docker registry URL
    -t, --tag TAG           Docker image tag [default: latest]
    -s, --skip-build        Skip building Docker images
    --skip-tests            Skip running tests
    --dry-run               Show what would be deployed without actually deploying
    -h, --help              Show this help message

Examples:
    $0 -e production -r myregistry.com -t v2.0.1
    $0 --environment staging --skip-build
    $0 --dry-run -e production

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT"
    print_error "Must be one of: development, staging, production"
    exit 1
fi

# Set registry based on environment if not provided
if [[ -z "$REGISTRY" ]]; then
    case $ENVIRONMENT in
        development)
            REGISTRY="localhost:5000"
            ;;
        staging)
            REGISTRY="staging-registry.example.com"
            ;;
        production)
            REGISTRY="registry.example.com"
            ;;
    esac
fi

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if docker is available (unless skipping build)
    if [[ "$SKIP_BUILD" == false ]] && ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    # Check if namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        print_warning "Namespace '$NAMESPACE' does not exist. Creating it..."
        if [[ "$DRY_RUN" == false ]]; then
            kubectl create namespace "$NAMESPACE"
        fi
    fi
    
    print_success "Prerequisites check passed"
}

# Function to run tests
run_tests() {
    if [[ "$SKIP_TESTS" == true ]]; then
        print_warning "Skipping tests"
        return
    fi
    
    print_status "Running tests..."
    
    # Backend tests
    print_status "Running backend tests..."
    cd backend
    if [[ "$DRY_RUN" == false ]]; then
        ./mvnw test
    else
        print_status "DRY RUN: Would run './mvnw test'"
    fi
    cd ..
    
    # Frontend tests
    print_status "Running frontend tests..."
    cd frontend
    if [[ "$DRY_RUN" == false ]]; then
        npm test -- --watchAll=false
    else
        print_status "DRY RUN: Would run 'npm test -- --watchAll=false'"
    fi
    cd ..
    
    print_success "All tests passed"
}

# Function to build Docker images
build_images() {
    if [[ "$SKIP_BUILD" == true ]]; then
        print_warning "Skipping image build"
        return
    fi
    
    print_status "Building Docker images..."
    
    # Build backend image
    print_status "Building backend image..."
    cd backend
    if [[ "$DRY_RUN" == false ]]; then
        ./mvnw clean package -DskipTests
        docker build --platform linux/amd64 -t "${REGISTRY}/kafka-web-app-backend:${TAG}" .
        docker push "${REGISTRY}/kafka-web-app-backend:${TAG}"
    else
        print_status "DRY RUN: Would build and push ${REGISTRY}/kafka-web-app-backend:${TAG}"
    fi
    cd ..
    
    # Build frontend image
    print_status "Building frontend image..."
    cd frontend
    if [[ "$DRY_RUN" == false ]]; then
        npm run build
        docker build --platform linux/amd64 -t "${REGISTRY}/kafka-web-app-frontend:${TAG}" .
        docker push "${REGISTRY}/kafka-web-app-frontend:${TAG}"
    else
        print_status "DRY RUN: Would build and push ${REGISTRY}/kafka-web-app-frontend:${TAG}"
    fi
    cd ..
    
    print_success "Images built and pushed successfully"
}

# Function to deploy to Kubernetes
deploy_kubernetes() {
    print_status "Deploying to Kubernetes..."
    
    # Create temporary deployment files with substituted values
    TEMP_DIR=$(mktemp -d)
    
    # Copy k8s files and substitute variables
    for file in k8s/*.yaml; do
        if [[ -f "$file" ]]; then
            filename=$(basename "$file")
            sed -e "s|{{REGISTRY}}|${REGISTRY}|g" \
                -e "s|{{TAG}}|${TAG}|g" \
                -e "s|{{NAMESPACE}}|${NAMESPACE}|g" \
                -e "s|{{ENVIRONMENT}}|${ENVIRONMENT}|g" \
                "$file" > "${TEMP_DIR}/${filename}"
        fi
    done
    
    # Apply environment-specific configurations
    if [[ -f "k8s/environments/${ENVIRONMENT}.yaml" ]]; then
        cp "k8s/environments/${ENVIRONMENT}.yaml" "${TEMP_DIR}/"
    fi
    
    # Deploy to Kubernetes
    if [[ "$DRY_RUN" == false ]]; then
        kubectl apply -f "${TEMP_DIR}/" -n "$NAMESPACE"
    else
        print_status "DRY RUN: Would apply the following files:"
        ls -la "${TEMP_DIR}/"
        print_status "DRY RUN: kubectl apply -f ${TEMP_DIR}/ -n ${NAMESPACE}"
    fi
    
    # Clean up temporary files
    rm -rf "$TEMP_DIR"
    
    print_success "Deployment completed"
}

# Function to wait for deployment
wait_for_deployment() {
    if [[ "$DRY_RUN" == true ]]; then
        print_status "DRY RUN: Would wait for deployment to be ready"
        return
    fi
    
    print_status "Waiting for deployment to be ready..."
    
    # Wait for backend deployment
    kubectl rollout status deployment/kafka-web-app-backend -n "$NAMESPACE" --timeout=300s
    
    # Wait for frontend deployment
    kubectl rollout status deployment/kafka-web-app-frontend -n "$NAMESPACE" --timeout=300s
    
    print_success "Deployment is ready"
}

# Function to run health checks
run_health_checks() {
    if [[ "$DRY_RUN" == true ]]; then
        print_status "DRY RUN: Would run health checks"
        return
    fi
    
    print_status "Running health checks..."
    
    # Get service URLs
    BACKEND_URL=$(kubectl get service kafka-web-app-backend -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "localhost")
    
    # Check backend health
    print_status "Checking backend health..."
    for i in {1..30}; do
        if kubectl exec -n "$NAMESPACE" deployment/kafka-web-app-backend -- curl -f http://localhost:8080/actuator/health &>/dev/null; then
            print_success "Backend health check passed"
            break
        fi
        if [[ $i -eq 30 ]]; then
            print_error "Backend health check failed after 30 attempts"
            exit 1
        fi
        sleep 10
    done
    
    # Check frontend accessibility
    print_status "Checking frontend accessibility..."
    for i in {1..30}; do
        if kubectl exec -n "$NAMESPACE" deployment/kafka-web-app-frontend -- curl -f http://localhost/ &>/dev/null; then
            print_success "Frontend accessibility check passed"
            break
        fi
        if [[ $i -eq 30 ]]; then
            print_error "Frontend accessibility check failed after 30 attempts"
            exit 1
        fi
        sleep 10
    done
    
    print_success "All health checks passed"
}

# Function to show deployment info
show_deployment_info() {
    print_status "Deployment Information:"
    echo "  Environment: $ENVIRONMENT"
    echo "  Namespace: $NAMESPACE"
    echo "  Registry: $REGISTRY"
    echo "  Tag: $TAG"
    echo ""
    
    if [[ "$DRY_RUN" == false ]]; then
        print_status "Deployed Resources:"
        kubectl get all -n "$NAMESPACE"
        
        # Show ingress if available
        if kubectl get ingress -n "$NAMESPACE" &>/dev/null; then
            echo ""
            print_status "Ingress Information:"
            kubectl get ingress -n "$NAMESPACE"
        fi
    fi
}

# Main deployment flow
main() {
    print_status "Starting Kafka Web Tool v2.0 deployment..."
    print_status "Environment: $ENVIRONMENT"
    print_status "Namespace: $NAMESPACE"
    print_status "Registry: $REGISTRY"
    print_status "Tag: $TAG"
    
    if [[ "$DRY_RUN" == true ]]; then
        print_warning "DRY RUN MODE - No actual changes will be made"
    fi
    
    check_prerequisites
    run_tests
    build_images
    deploy_kubernetes
    wait_for_deployment
    run_health_checks
    show_deployment_info
    
    print_success "Deployment completed successfully!"
    
    if [[ "$ENVIRONMENT" == "production" ]]; then
        print_warning "Production deployment completed. Please verify all systems are working correctly."
    fi
}

# Run main function
main "$@"
