#!/bin/bash

# Kafka Web Tool v2.0 Quick Start Script
# This script helps you get started quickly with the Kafka Web Tool

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Function to show banner
show_banner() {
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════╗
║                    Kafka Web Tool v2.0                      ║
║                      Quick Start                            ║
╚══════════════════════════════════════════════════════════════╝
EOF
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        print_status "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        print_status "Visit: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Function to setup environment
setup_environment() {
    print_status "Setting up environment..."
    
    # Create .env file if it doesn't exist
    if [[ ! -f .env ]]; then
        print_status "Creating .env file from template..."
        cp .env.example .env
        
        # Generate secure passwords
        DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
        JWT_SECRET=$(openssl rand -base64 32)
        
        # Update .env file with generated passwords
        sed -i.bak "s/DB_PASSWORD=kafka-web-tool-change-me/DB_PASSWORD=${DB_PASSWORD}/" .env
        sed -i.bak "s/JWT_SECRET=your-256-bit-secret-change-me-in-production/JWT_SECRET=${JWT_SECRET}/" .env
        
        # Remove backup file
        rm -f .env.bak
        
        print_success "Environment file created with secure passwords"
    else
        print_warning ".env file already exists, skipping creation"
    fi
    
    # Create logs directory
    mkdir -p logs
    
    print_success "Environment setup completed"
}

# Function to start services
start_services() {
    print_status "Starting Kafka Web Tool services..."
    
    # Pull latest images
    print_status "Pulling Docker images..."
    docker-compose pull
    
    # Start services
    print_status "Starting services..."
    docker-compose up -d
    
    print_success "Services started successfully"
}

# Function to wait for services
wait_for_services() {
    print_status "Waiting for services to be ready..."
    
    # Wait for database
    print_status "Waiting for database..."
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U postgres -d kafka_web_tool &>/dev/null; then
            print_success "Database is ready"
            break
        fi
        if [[ $i -eq 30 ]]; then
            print_error "Database failed to start after 5 minutes"
            exit 1
        fi
        sleep 10
    done
    
    # Wait for backend
    print_status "Waiting for backend..."
    for i in {1..60}; do
        if curl -f http://localhost:8080/actuator/health &>/dev/null; then
            print_success "Backend is ready"
            break
        fi
        if [[ $i -eq 60 ]]; then
            print_error "Backend failed to start after 10 minutes"
            exit 1
        fi
        sleep 10
    done
    
    # Wait for frontend
    print_status "Waiting for frontend..."
    for i in {1..30}; do
        if curl -f http://localhost:3000 &>/dev/null; then
            print_success "Frontend is ready"
            break
        fi
        if [[ $i -eq 30 ]]; then
            print_error "Frontend failed to start after 5 minutes"
            exit 1
        fi
        sleep 10
    done
    
    print_success "All services are ready!"
}

# Function to show service status
show_status() {
    print_status "Service Status:"
    docker-compose ps
    
    echo ""
    print_status "Service Health:"
    
    # Check database
    if docker-compose exec -T postgres pg_isready -U postgres -d kafka_web_tool &>/dev/null; then
        echo -e "  Database: ${GREEN}✓ Healthy${NC}"
    else
        echo -e "  Database: ${RED}✗ Unhealthy${NC}"
    fi
    
    # Check backend
    if curl -f http://localhost:8080/actuator/health &>/dev/null; then
        echo -e "  Backend:  ${GREEN}✓ Healthy${NC}"
    else
        echo -e "  Backend:  ${RED}✗ Unhealthy${NC}"
    fi
    
    # Check frontend
    if curl -f http://localhost:3000 &>/dev/null; then
        echo -e "  Frontend: ${GREEN}✓ Healthy${NC}"
    else
        echo -e "  Frontend: ${RED}✗ Unhealthy${NC}"
    fi
}

# Function to show access information
show_access_info() {
    echo ""
    print_success "🎉 Kafka Web Tool is ready!"
    echo ""
    echo "Access URLs:"
    echo "  📊 Web Application: http://localhost:3000"
    echo "  🔧 Backend API:     http://localhost:8080"
    echo "  📋 API Docs:       http://localhost:8080/swagger-ui.html"
    echo "  💾 Database:       localhost:5432 (postgres/kafka_web_tool)"
    echo ""
    echo "Default Credentials:"
    echo "  Username: admin"
    echo "  Password: admin"
    echo ""
    echo "Next Steps:"
    echo "  1. Open http://localhost:3000 in your browser"
    echo "  2. Add your Kafka cluster connections"
    echo "  3. Start managing your Kafka topics and messages"
    echo ""
    echo "Documentation:"
    echo "  📖 Full Documentation: ./docs/"
    echo "  🚀 Deployment Guide:   ./docs/DEPLOYMENT.md"
    echo "  ⚙️  Configuration:      ./docs/CONFIGURATION.md"
    echo "  🔧 Troubleshooting:    ./docs/TROUBLESHOOTING.md"
    echo ""
    echo "Useful Commands:"
    echo "  View logs:    docker-compose logs -f"
    echo "  Stop:         docker-compose down"
    echo "  Restart:      docker-compose restart"
    echo "  Update:       docker-compose pull && docker-compose up -d"
}

# Function to show help
show_help() {
    cat << EOF
Kafka Web Tool v2.0 Quick Start Script

Usage: $0 [COMMAND]

Commands:
    start       Start all services (default)
    stop        Stop all services
    restart     Restart all services
    status      Show service status
    logs        Show service logs
    update      Update to latest version
    clean       Clean up all data (CAUTION: Data loss)
    help        Show this help message

Examples:
    $0              # Start services
    $0 start        # Start services
    $0 stop         # Stop services
    $0 status       # Show status
    $0 logs         # Show logs

EOF
}

# Function to stop services
stop_services() {
    print_status "Stopping Kafka Web Tool services..."
    docker-compose down
    print_success "Services stopped"
}

# Function to restart services
restart_services() {
    print_status "Restarting Kafka Web Tool services..."
    docker-compose restart
    wait_for_services
    show_access_info
}

# Function to show logs
show_logs() {
    print_status "Showing service logs (Press Ctrl+C to exit)..."
    docker-compose logs -f
}

# Function to update services
update_services() {
    print_status "Updating Kafka Web Tool to latest version..."
    docker-compose pull
    docker-compose up -d
    wait_for_services
    print_success "Update completed"
    show_access_info
}

# Function to clean up
clean_services() {
    print_warning "This will remove all data including database content!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status "Cleaning up all data..."
        docker-compose down -v
        docker system prune -f
        rm -rf logs/*
        print_success "Cleanup completed"
    else
        print_status "Cleanup cancelled"
    fi
}

# Main function
main() {
    show_banner
    
    case "${1:-start}" in
        start)
            check_prerequisites
            setup_environment
            start_services
            wait_for_services
            show_status
            show_access_info
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        update)
            update_services
            ;;
        clean)
            clean_services
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
