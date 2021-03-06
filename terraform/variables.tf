variable "env" {
  description = "environment"
  default     = ""
}

variable "region" {
  description = "region"
  default     = "us-west-2"
}

variable "enclave" {
  default = "shared"
}

variable "service" {
  description = "name of the service"
  default     = "conductor"
}

variable "cluster_size" {
  default = {
    test = 1
    int  = 1
    uat  = 1
    live = 1
  }
}

variable "engine" {
  default = "aurora-postgresql"
}

variable "engine_version" {
  default = "10.6"
}

variable "instance_class" {
  default = {
    test = "db.r5.large"
    int  = "db.r5.4xlarge"
    uat  = "db.r5.4xlarge"
    live = "db.r5.4xlarge"
  }
}

variable "apply_immediately" {
  default = true
}

variable "publicly_accessible" {
  default = false
}

variable "availability_zones" {
  default = ["us-west-2a", "us-west-2b", "us-west-2c"]
}

variable "database_port" {
  default = "5432"
}

variable "owner" {
  default = "conductor"
}

variable "database_name" {
  default = {
    test = "conductor_development"
    int  = "conductor_integration"
    uat  = "conductor_uat"
    live = "conductor_production"
  }
}

variable "database_user" {
  default = {
    test = "conductor_development"
    int  = "conductor_integration"
    uat  = "conductor_uat"
    live = "conductor_production"
  }
}

variable "allowed_subnets" {
  description = "Allowed subnets for security groups."

  default = {
    all             = ["0.0.0.0/0"]
    all-10-priv     = ["10.0.0.0/8"]
    all-172-priv    = ["172.16.0.0/12"]
    dlx-coresite    = ["192.168.0.0/16"]
    shared-test-vpc = ["172.31.96.0/20"]
    shared-live-vpc = ["10.131.96.0/20"]
  }
}

variable "generate_new_password" {
  description = "when this value is changed from what is in tf_state, a new password for db will be generated"
  default     = "1"
}

module "vars" {
  source  = "github.com/d3sw/terraform-platform//modules/vars"
  region  = "${var.region}"
  env     = "${var.env}"
  enclave = "${var.enclave}"
  service = "${var.service}"
}
