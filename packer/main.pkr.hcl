packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = "~> 1"
    }
  }
}

variable "artifact_url" {
  description = "URL where the JAR file is stored"
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

locals {
  ami_name = "${var.ami_name_prefix}-${formatdate("YYYYMMDD-HHmm", timestamp())}"
}

source "amazon-ebs" "ubuntu" {
  instance_type = var.instance_type
  ami_name      = local.ami_name
  ssh_username  = "ubuntu"
  source_ami    = "ami-0866a3c8686eaeeba"
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  # 1. Install Java 17, PostgreSQL, and other dependencies
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

      # Set up PostgreSQL database with variables
      "sudo -u postgres psql -c \"CREATE DATABASE ${var.db_name};\"",
      "sudo -u postgres psql -c \"CREATE USER ${var.db_username} WITH PASSWORD '${var.db_password}';\"",
      "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE ${var.db_name} TO ${var.db_username};\""
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

  # 4. Move it to /opt/myapp
  provisioner "shell" {
    inline = [
      "sudo curl -L -o /opt/myapp/webapp.jar ${var.artifact_url}",
      "sudo chown csye6225:csye6225 /opt/myapp/webapp.jar"
    ]
  }

  # 5. Set environment variables and configure the systemd service
  provisioner "shell" {
    inline = [
      # Set environment variables
      "echo 'DB_URL=${var.db_name}' | sudo tee -a /etc/environment",
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
