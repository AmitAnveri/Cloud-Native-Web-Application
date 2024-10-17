packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = "~> 1"
    }
  }
}

variable "artifact_path" {
  description = "The path to the application artifact JAR file"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
}

variable "ami_name_prefix" {
  description = "Prefix for the AMI name"
  type        = string
  default     = "webapp-ami"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
}

variable "source_ami"{
  description = "EC2 source AMI id"
  type        = string
  default = "ami-0866a3c8686eaeeba"
}

locals {
  ami_name = "${var.ami_name_prefix}-${formatdate("YYYYMMDD-HHmm", timestamp())}"
}

source "amazon-ebs" "ubuntu" {
  instance_type = var.instance_type
  ami_name      = local.ami_name
  ssh_username  = "ubuntu"
  source_ami    = var.source_ami
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  # 1. Install Java 17, PostgreSQL
  provisioner "shell" {
    inline = [
      "sudo apt-get update",
      "sudo apt-get upgrade -y",
      "sudo apt-get install -y openjdk-17-jdk postgresql"
    ]
  }

  # 2. Set up PostgreSQL database and configure access using variables
  provisioner "shell" {
    inline = [
      "sudo systemctl start postgresql",
      "sudo systemctl enable postgresql",

      # Set up PostgreSQL database with variables and grant necessary privileges
      "sudo -u postgres psql -c \"CREATE DATABASE ${var.db_name};\"",
      "sudo -u postgres psql -c \"CREATE USER ${var.db_username} WITH PASSWORD '${var.db_password}';\"",

      # Grant all privileges to the user on the database
      "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE ${var.db_name} TO ${var.db_username};\"",

      # Grant SUPERUSER privileges to ensure full control (can create schemas, tables, etc.)
      "sudo -u postgres psql -c \"ALTER USER ${var.db_username} WITH SUPERUSER;\"",

      # Explicitly grant CREATE privilege for schemas
      "sudo -u postgres psql -c \"GRANT CREATE ON DATABASE ${var.db_name} TO ${var.db_username};\""
    ]
  }

  # 3. Create the non-login user csye6225 and set directory permissions
  provisioner "shell" {
    inline = [
      "sudo adduser --system --group --no-create-home --shell /usr/sbin/nologin csye6225",
      "sudo mkdir -p /opt/myapp",
      "sudo chown csye6225:csye6225 /opt/myapp",
      "sudo chmod 755 /opt/myapp"
    ]
  }

  # 4. Copy the JAR from the local file to the instance
  provisioner "file" {
    source      = var.artifact_path
    destination = "/tmp/webapp.jar"
  }

  provisioner "shell" {
    inline = [
      "sudo mv /tmp/webapp.jar /opt/myapp/webapp.jar",
      "sudo chown csye6225:csye6225 /opt/myapp/webapp.jar",
      "sudo chmod 755 /opt/myapp/webapp.jar"
    ]
  }

  # 5. Set environment variables and configure the systemd service
  provisioner "shell" {
    inline = [
      # Set full DB_URL with host and port
      "echo 'DB_URL=jdbc:postgresql://localhost:5432/${var.db_name}' | sudo tee -a /etc/environment",
      "echo 'DB_USERNAME=${var.db_username}' | sudo tee -a /etc/environment",
      "echo 'DB_PASSWORD=${var.db_password}' | sudo tee -a /etc/environment",

      # Create a systemd service to run the application
      "sudo bash -c 'cat <<EOF > /etc/systemd/system/webapp.service\n[Unit]\nDescription=CSYE6225 WebApp\nAfter=network.target\n\n[Service]\nUser=csye6225\nExecStart=/usr/bin/java -jar /opt/myapp/webapp.jar\nRestart=always\nEnvironmentFile=/etc/environment\n\n[Install]\nWantedBy=multi-user.target\nEOF'",

      # Reload systemd and enable the service
      "sudo systemctl daemon-reload",
      "sudo systemctl enable webapp.service"
    ]
  }
}
