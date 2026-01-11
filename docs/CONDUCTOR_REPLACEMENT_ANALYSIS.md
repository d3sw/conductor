# Workflow Orchestration Platform Replacement Analysis

## Executive Summary

This document provides an exhaustive analysis of workflow orchestration platforms being considered as replacements for the d3sw/conductor fork. The current fork has accumulated significant business logic customizations that prevent accepting upstream fixes and improvements from the original Netflix Conductor repository.

### Current State: d3sw/conductor Fork

The existing codebase is a heavily customized fork with the following key characteristics:

| Aspect | Details |
|--------|---------|
| **Primary Persistence** | PostgreSQL/Aurora (via HikariCP, Flyway migrations) |
| **Secondary Persistence** | Elasticsearch 5/6 for indexing, Redis/Dynomite |
| **Message Queues** | Shotgun/OneMQ (primary), NATS, AWS SQS |
| **Custom Integrations** | Correlator (context propagation), StatusEventPublisher, AssetMonitor, SherlockBatchProcessor |
| **Authentication** | OAuth2/JWT integration with custom AuthManager |
| **Task Types** | 23+ system tasks including custom domain-specific tasks |
| **Deployment** | HashiCorp Nomad + Vault + Terraform |
| **Java Version** | Java 8 |

**Critical Customizations to Preserve:**
1. PostgreSQL backend with Flyway schema migrations (V1-V8+)
2. Shotgun/OneMQ queue integration for message passing
3. Correlator with `Deluxe-Owf-Context` HTTP header propagation
4. Custom workflow conditions (FeatureCondition, MetadataStatusAtlasIdCondition, TitleKeysActionCondition, etc.)
5. AssetMonitor for asset/deliverable workflow management
6. Archiver service for historical workflow storage
7. Custom authentication framework

---

## Candidates Overview

| Platform | Type | License | Primary Use Case | Vendor Lock-in |
|----------|------|---------|------------------|----------------|
| **Conductor OSS** | Self-hosted | Apache 2.0 | Microservices orchestration | None |
| **Orkes Conductor** | Managed/Self-hosted | Commercial | Enterprise microservices | Low |
| **Temporal** | Self-hosted/Cloud | MIT | Durable workflows | Low |
| **AWS Step Functions** | Managed | Proprietary | AWS-native workflows | High (AWS) |
| **Camunda 8** | Self-hosted/Cloud | SSPL/Commercial | BPM & microservices | Low |
| **Argo Workflows** | Self-hosted | Apache 2.0 | Kubernetes-native CI/CD | Kubernetes |
| **Azure Durable Functions** | Managed | Proprietary | Azure serverless | High (Azure) |
| **Apache Airflow** | Self-hosted/Managed | Apache 2.0 | Data pipelines/ETL | None |

---

## Detailed Analysis

### 1. Conductor OSS (conductor-oss/conductor)

**Overview:**
The community-maintained fork of Netflix Conductor after Netflix discontinued maintenance in December 2023. Now actively maintained by Orkes and the open-source community.

**Repository:** [conductor-oss/conductor](https://github.com/conductor-oss/conductor) (31,610+ GitHub stars)

#### Strengths

- **Minimal Migration Effort**: Closest to current d3sw/conductor codebase; potential to merge customizations
- **Active Community**: Maintained by original Conductor creators at Orkes with regular updates
- **Familiar Architecture**: Same JSON DSL workflow definitions, task model, and API surface
- **Multiple Persistence Options**: PostgreSQL, Redis, Elasticsearch, Cassandra, MySQL
- **Language Agnostic Workers**: SDKs for Java, Python, Go, C#, JavaScript
- **Visual Workflow Editor**: Built-in UI for workflow design and monitoring
- **No License Cost**: Apache 2.0 open source

#### Weaknesses

- **Self-Managed Operations**: Full DevOps responsibility for scaling, upgrades, monitoring
- **Feature Lag vs Orkes**: Commercial features (AI/LLM chaining, advanced security) not in OSS
- **Community Size**: Smaller than alternatives like Temporal or Airflow
- **Documentation**: Can be sparse for advanced customization scenarios

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | Low | Compatible JSON DSL |
| Task Workers | Low | Same worker model |
| Persistence Layer | Medium | PostgreSQL supported; may need schema adjustments |
| Custom Integrations | Medium | Shotgun/OneMQ requires custom queue implementation |
| Authentication | Medium | Need to port AuthManager integration |

#### Pricing

| Deployment | Cost |
|------------|------|
| Self-hosted | Infrastructure only (~$2-10K/month depending on scale) |

#### Best For
Teams wanting to stay on Conductor architecture with minimal disruption while gaining access to upstream improvements.

---

### 2. Orkes Conductor (Commercial)

**Overview:**
Enterprise-grade managed Conductor platform from the original Netflix Conductor creators. Offers both cloud-hosted and self-managed deployment options.

**Website:** [orkes.io](https://orkes.io)

#### Strengths

- **Full Conductor Compatibility**: Drop-in replacement for Conductor OSS
- **Enterprise Features**:
  - Advanced RBAC with SSO/SAML integration
  - Audit logs and change data capture
  - Multi-region high availability
  - Up to 60,000 parallel forks per workflow
  - AI/LLM task orchestration built-in
  - Human task management
- **Operational Excellence**: Managed infrastructure, automatic scaling, SLA guarantees
- **Expert Support**: Direct access to Conductor creators
- **Flexible Deployment**: Cloud, private cloud, or on-premises
- **Visual Workflow Designer**: Enhanced UI with drag-and-drop workflow creation

#### Weaknesses

- **Cost**: Significant licensing/subscription fees for enterprise features
- **Vendor Dependency**: Reliance on Orkes for support and enterprise features
- **Pricing Transparency**: Custom pricing requires sales engagement

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | Low | 100% compatible |
| Task Workers | Low | Existing workers work unchanged |
| Persistence Layer | Low | Orkes handles persistence |
| Custom Integrations | Medium | May need to adapt to Orkes patterns |
| Authentication | Low | Built-in SSO/SAML support |

#### Pricing

| Tier | Starting Price | Notes |
|------|----------------|-------|
| Developer Playground | Free | Not for production, no SLA |
| Enterprise | Custom | Contact sales; cluster-based pricing |

**Key Differentiator**: Cluster-based pricing vs. Temporal's per-action pricing means predictable costs at scale.

#### Best For
Organizations wanting managed Conductor with enterprise security, support, and no operational burden.

---

### 3. Temporal

**Overview:**
Durable execution platform evolved from Uber's Cadence project. Emphasizes code-first workflow definitions with automatic state persistence and failure recovery.

**Website:** [temporal.io](https://temporal.io)
**Repository:** [temporalio/temporal](https://github.com/temporalio/temporal) (13,000+ GitHub stars)

#### Strengths

- **Durable Execution**: Automatic state checkpointing; workflows survive failures and resume exactly where they left off
- **Code-First Workflows**: Write workflows in native programming languages (Go, Java, Python, TypeScript, .NET)
- **Strong Consistency**: Event sourcing provides complete audit trail and replay capability
- **Production Proven**: Used by Stripe, Netflix, Datadog, Snap, Coinbase
- **Active Development**: Rapid feature development and strong community
- **Multi-Language Support**: Native SDKs for all major languages
- **Long-Running Workflows**: Can run for months/years with full state preservation

#### Weaknesses

- **Paradigm Shift**: Code-first approach requires rewriting all workflow definitions (no JSON DSL)
- **Complexity**: Steeper learning curve; requires understanding event sourcing model
- **Resource Intensive**: Self-hosted requires careful tuning; can be resource-heavy
- **Cloud Pricing**: Usage-based pricing can become expensive at scale (starts at $50/million actions)

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must rewrite all workflows in code |
| Task Workers | Medium | Different activity model; conceptually similar |
| Persistence Layer | Low | Temporal handles persistence internally |
| Custom Integrations | High | Must rebuild integrations using Temporal patterns |
| Authentication | Medium | Different auth model |

#### Pricing

| Option | Cost | Notes |
|--------|------|-------|
| Self-hosted | Infrastructure + DevOps | Significant operational investment |
| Temporal Cloud Essentials | $100/month + usage | Actions: $50/M (first 5M) |
| Temporal Cloud Business | $500/month + usage | Priority support, higher limits |
| Temporal Cloud Enterprise | Custom | Dedicated support, SLAs |

**Real-World Example**: Attentive reported $30,000/month savings migrating from self-hosted to Temporal Cloud.

#### Best For
Teams building new applications requiring bulletproof reliability, or those comfortable with significant rewrite investment for long-term benefits.

---

### 4. AWS Step Functions

**Overview:**
Fully managed serverless workflow service from AWS. Uses Amazon States Language (JSON-based) for workflow definitions with deep integration into the AWS ecosystem.

**Documentation:** [AWS Step Functions](https://aws.amazon.com/step-functions/)

#### Strengths

- **Zero Operations**: Fully managed; no infrastructure to maintain
- **AWS Integration**: Native integration with Lambda, ECS, SQS, SNS, DynamoDB, API Gateway, etc.
- **Visual Workflow Designer**: Drag-and-drop workflow creation in AWS Console
- **Pay-Per-Use**: Only pay for state transitions
- **High Availability**: Built-in redundancy across AWS availability zones
- **Express Workflows**: High-throughput option for short-duration workflows

#### Weaknesses

- **Vendor Lock-in**: Deeply tied to AWS; difficult to migrate away
- **Cost at Scale**: State transition pricing can explode with high-volume workflows
- **Limited Flexibility**: Amazon States Language is more restrictive than code-based workflows
- **Cold Start Latency**: Can experience delays when integrating with Lambda
- **No On-Premises**: AWS-only deployment

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must rewrite in Amazon States Language |
| Task Workers | **High** | Must use Lambda, ECS, or integrated services |
| Persistence Layer | N/A | AWS managed |
| Custom Integrations | **High** | Must rebuild using AWS services |
| Authentication | Medium | IAM-based; different model |

#### Pricing

| Type | Cost |
|------|------|
| Standard Workflows | $0.025 per 1,000 state transitions |
| Express Workflows | $0.00001 per request + duration |

**Example**: 1 million workflow executions with 10 states each = $250/month (standard)

#### Best For
AWS-native organizations with workflows that primarily orchestrate AWS services and can accept vendor lock-in.

---

### 5. Camunda 8

**Overview:**
Cloud-native process orchestration platform built on the Zeebe engine. Combines BPMN-based workflow design with microservices orchestration capabilities.

**Website:** [camunda.com](https://camunda.com)
**Repository:** [camunda/camunda](https://github.com/camunda/camunda)

#### Strengths

- **BPMN 2.0 Standard**: Industry-standard workflow notation; portable workflows
- **Horizontal Scaling**: Zeebe engine scales to thousands of instances per second
- **Cloud-Native**: Kubernetes-optimized, event streaming architecture
- **Visual Modeling**: Powerful BPMN modeler for workflow design
- **Human Task Management**: Built-in tasklist for human-in-the-loop workflows
- **DMN Support**: Decision Model and Notation for business rules
- **Polyglot Workers**: Connectors for multiple languages and systems
- **AI/Agent Orchestration**: New agentic capabilities added in 2025

#### Weaknesses

- **Complexity**: Significant learning curve for BPMN and Camunda concepts
- **Migration Effort**: Different paradigm from Conductor; requires workflow redesign
- **Licensing**: SSPL for self-hosted; commercial license for enterprise features
- **Operational Overhead**: Self-hosted Camunda 8 is complex to operate

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must redesign using BPMN |
| Task Workers | Medium | Job worker model is conceptually similar |
| Persistence Layer | Low | Camunda handles persistence (Elasticsearch + Zeebe) |
| Custom Integrations | High | Must rebuild as Camunda Connectors |
| Authentication | Medium | Different auth model; SSO supported |

#### Pricing

| Option | Cost |
|--------|------|
| Self-hosted | Infrastructure + optional support contract |
| Camunda Cloud | Contact sales for enterprise pricing |

#### Best For
Organizations wanting standardized BPMN workflows, human task management, and enterprise BPM capabilities.

---

### 6. Argo Workflows

**Overview:**
Kubernetes-native workflow engine implemented as a Custom Resource Definition (CRD). Each workflow step runs as a Kubernetes pod.

**Website:** [argoproj.github.io/workflows](https://argoproj.github.io/workflows/)
**Repository:** [argoproj/argo-workflows](https://github.com/argoproj/argo-workflows) (13,000+ GitHub stars)

#### Strengths

- **Kubernetes Native**: Leverages Kubernetes for scheduling, scaling, and resource management
- **Cloud Agnostic**: Works on any Kubernetes cluster; no vendor lock-in
- **Container-Based Tasks**: Each step runs in an isolated container
- **Artifact Passing**: Built-in support for passing data between steps
- **CNCF Graduated**: Production-ready with strong community support
- **CI/CD Integration**: Pairs well with Argo CD for GitOps workflows
- **ML/Data Pipelines**: Popular choice for ML training and data processing

#### Weaknesses

- **Kubernetes Required**: Must run on Kubernetes; not suitable for non-K8s environments
- **Stateless Focus**: Better suited for batch/CI/CD than long-running stateful workflows
- **Limited Human Tasks**: No built-in human task management
- **YAML-Heavy**: Workflow definitions can become verbose and complex

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must rewrite as Kubernetes YAML/CRDs |
| Task Workers | **High** | Must containerize all tasks |
| Persistence Layer | Medium | Kubernetes-based; configurable backend |
| Custom Integrations | High | Must rebuild as containers |
| Authentication | Medium | Kubernetes RBAC-based |

#### Pricing

| Deployment | Cost |
|------------|------|
| Self-hosted | Kubernetes cluster costs only |

#### Best For
Kubernetes-first organizations with batch processing, CI/CD, or ML pipeline workloads.

---

### 7. Azure Durable Functions

**Overview:**
Extension of Azure Functions enabling stateful workflows in a serverless environment. Code-first approach with automatic state checkpointing.

**Documentation:** [Azure Durable Functions](https://learn.microsoft.com/en-us/azure/azure-functions/durable/durable-functions-overview)

#### Strengths

- **Serverless**: Pay only for execution time; no infrastructure management
- **Code-First**: Write orchestrations in C#, JavaScript, Python, or PowerShell
- **Azure Integration**: Deep integration with Azure services (Service Bus, Event Grid, Blob Storage)
- **Automatic State Management**: Built-in checkpointing and replay
- **Sub-Orchestrations**: Compose complex workflows from smaller orchestrations
- **Human Interaction**: Built-in support for approval workflows

#### Weaknesses

- **Azure Lock-in**: Only runs on Azure; significant migration cost to leave
- **Language Limitations**: Best support for .NET; other language SDKs are less mature
- **Cold Starts**: Can experience latency on first execution
- **Debugging Complexity**: Replay-based execution can be tricky to debug

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must rewrite in supported language |
| Task Workers | **High** | Must convert to Azure Functions |
| Persistence Layer | N/A | Azure managed |
| Custom Integrations | **High** | Must use Azure services |
| Authentication | Medium | Azure AD integration |

#### Pricing

| SKU | Cost |
|-----|------|
| Consumption | Pay-per-execution (similar to Azure Functions pricing) |
| Durable Task Scheduler Dedicated | Predictable pricing for steady workloads |

#### Best For
Azure-native organizations seeking serverless workflow orchestration within the Azure ecosystem.

---

### 8. Apache Airflow

**Overview:**
Industry-standard platform for scheduling and orchestrating ETL/data pipelines. DAG-based workflow model with Python definitions.

**Website:** [airflow.apache.org](https://airflow.apache.org)
**Repository:** [apache/airflow](https://github.com/apache/airflow) (35,000+ GitHub stars)

#### Strengths

- **Industry Standard**: De facto standard for data engineering workflows
- **Rich Ecosystem**: 1000+ integrations (operators, hooks, sensors)
- **Python-Native**: Workflows defined in Python; familiar to data teams
- **Managed Options**: Available on AWS (MWAA), GCP (Cloud Composer), Astronomer
- **Scheduling**: Powerful scheduling with cron expressions and data-aware triggers
- **Extensive UI**: Web UI for monitoring, debugging, and manual triggers

#### Weaknesses

- **Batch-Oriented**: Not designed for real-time or event-driven workflows
- **No Native State Management**: Limited support for long-running stateful workflows
- **Scalability Challenges**: Can struggle with very high concurrency
- **Not for Microservices**: Better suited for data pipelines than service orchestration

#### Migration Considerations

| Aspect | Effort | Notes |
|--------|--------|-------|
| Workflow Definitions | **High** | Must rewrite as Python DAGs |
| Task Workers | Medium | Different operator/task model |
| Persistence Layer | Low | Configurable (PostgreSQL, MySQL) |
| Custom Integrations | Medium-High | Can write custom operators |
| Authentication | Low-Medium | RBAC, SSO options available |

#### Pricing

| Option | Cost |
|--------|------|
| Self-hosted | Infrastructure only |
| AWS MWAA | ~$0.49/hour per environment + worker costs |
| GCP Cloud Composer | ~$0.35/hour per environment + GKE costs |
| Astronomer | Contact for pricing |

#### Best For
Data engineering teams needing robust ETL/ELT pipeline orchestration with extensive data source integrations.

---

## Additional Alternatives Worth Considering

### Prefect

**Best For:** Python-first ML/data workflows with modern developer experience

| Pros | Cons |
|------|------|
| Minimal Python decorators to define flows | Less mature than Airflow |
| Excellent failure handling | Smaller ecosystem |
| Modern UI and developer experience | Not ideal for microservices orchestration |
| Hybrid cloud/self-hosted model | |

### Dagster

**Best For:** Asset-centric data pipelines with strong data lineage

| Pros | Cons |
|------|------|
| Asset-centric model fits data platforms | Steeper learning curve |
| Excellent DBT integration | Forces other workflow types into unnatural patterns |
| Strong data lineage and cataloging | |
| Growing rapidly in popularity | |

### Kestra

**Best For:** Data orchestration with declarative YAML workflows

| Pros | Cons |
|------|------|
| Event-driven with minimal code | Newer, smaller community |
| Plugin ecosystem | Less battle-tested |
| Real-time and batch support | |

---

## Comparison Matrix

### Feature Comparison

| Feature | Conductor OSS | Orkes | Temporal | Step Functions | Camunda 8 | Argo |
|---------|--------------|-------|----------|----------------|-----------|------|
| Workflow Definition | JSON DSL | JSON DSL | Code | ASL (JSON) | BPMN | YAML |
| Visual Designer | Yes | Enhanced | Limited | Yes | Yes | Basic |
| Multi-Language SDKs | Yes | Yes | Yes | N/A | Yes | YAML-based |
| Human Tasks | Basic | Advanced | Via activities | Via Lambda | Built-in | No |
| Long-Running Workflows | Yes | Yes | Excellent | Limited | Yes | Limited |
| Event-Driven | Yes | Yes | Yes | Yes | Yes | Basic |
| PostgreSQL Support | Yes | Yes | Yes | N/A | No (ES) | Yes |
| Self-Hosted | Yes | Yes | Yes | No | Yes | Yes |
| Managed Cloud | No | Yes | Yes | Yes | Yes | No |

### Operational Comparison

| Aspect | Conductor OSS | Orkes | Temporal | Step Functions | Camunda 8 | Argo |
|--------|--------------|-------|----------|----------------|-----------|------|
| DevOps Burden | High | Low | Medium-High | None | Medium-High | Medium |
| Kubernetes Required | No | No | No | No | Recommended | Yes |
| Multi-Region HA | DIY | Built-in | Cloud only | Built-in | Available | DIY |
| Monitoring | Basic | Advanced | Good | CloudWatch | Good | Prometheus |

### Migration Effort Comparison

| From d3sw/conductor | Effort | Timeline | Risk |
|---------------------|--------|----------|------|
| **Conductor OSS** | Low-Medium | 2-4 months | Low |
| **Orkes** | Low | 1-2 months | Low |
| **Temporal** | High | 6-12 months | Medium |
| **AWS Step Functions** | Very High | 9-18 months | High |
| **Camunda 8** | High | 6-12 months | Medium |
| **Argo Workflows** | Very High | 9-18 months | High |

---

## Recommendations

### Recommendation 1: Orkes Conductor (Lowest Risk)

**For teams prioritizing:**
- Minimal disruption to existing workflows
- Enterprise support and SLAs
- Predictable pricing at scale

**Rationale:**
- Direct compatibility with existing workflow definitions
- Existing task workers continue to work
- Enterprise security features (SSO, RBAC, audit logs) included
- Support from original Conductor creators
- Cluster-based pricing is predictable

**Migration Path:**
1. Evaluate Orkes with existing workflow definitions
2. Port custom queue integrations (Shotgun/OneMQ)
3. Configure authentication integration
4. Migrate workloads incrementally
5. Retire d3sw/conductor infrastructure

---

### Recommendation 2: Conductor OSS (Lowest Cost)

**For teams prioritizing:**
- Open source with no licensing costs
- Full control over infrastructure
- Ability to contribute fixes upstream

**Rationale:**
- Minimal workflow definition changes
- Access to upstream improvements and community fixes
- No vendor dependency
- Can merge d3sw customizations into OSS fork

**Migration Path:**
1. Fork conductor-oss/conductor
2. Port PostgreSQL schema and Flyway migrations
3. Implement Shotgun/OneMQ queue adapter
4. Port custom task types and conditions
5. Run parallel environments during migration

---

### Recommendation 3: Temporal (Long-term Investment)

**For teams prioritizing:**
- Best-in-class reliability and durability
- Modern code-first development experience
- Strong typing and IDE support

**Rationale:**
- Industry-leading durable execution guarantees
- Active development and growing ecosystem
- Better long-term maintainability with code-defined workflows
- Strong production track record at scale

**Trade-offs:**
- Requires complete workflow rewrite
- Significant learning curve for team
- Higher upfront investment for long-term gains

**Migration Path:**
1. Pilot new workflows on Temporal
2. Develop migration tooling for workflow conversion
3. Retrain development teams
4. Migrate workflows incrementally by domain
5. Run hybrid environment during transition

---

## Decision Framework

```
START
  |
  v
Is minimizing migration effort the top priority?
  |
  +-- YES --> Is budget available for commercial support?
  |              |
  |              +-- YES --> **ORKES CONDUCTOR**
  |              |
  |              +-- NO --> **CONDUCTOR OSS**
  |
  +-- NO --> Is the team already using Kubernetes extensively?
                |
                +-- YES --> Is the workload primarily batch/CI/CD?
                |              |
                |              +-- YES --> **ARGO WORKFLOWS**
                |              |
                |              +-- NO --> Continue below
                |
                +-- NO --> Continue below
                              |
                              v
                Is vendor lock-in acceptable?
                              |
                              +-- YES (AWS) --> **AWS STEP FUNCTIONS**
                              |
                              +-- YES (Azure) --> **AZURE DURABLE FUNCTIONS**
                              |
                              +-- NO --> Is investing in long-term platform OK?
                                            |
                                            +-- YES --> **TEMPORAL**
                                            |
                                            +-- NO --> **CONDUCTOR OSS / ORKES**
```

---

## Migration Risk Assessment

### High-Risk Factors

1. **Shotgun/OneMQ Integration**: Custom message queue not natively supported by alternatives
2. **Correlator Context Propagation**: Custom HTTP header handling for distributed tracing
3. **Custom Workflow Conditions**: Domain-specific condition handlers (TitleKeysActionCondition, etc.)
4. **AssetMonitor**: Complex event handler with external system dependencies
5. **PostgreSQL Schema**: Custom Flyway migrations with domain-specific tables

### Mitigation Strategies

| Risk | Mitigation |
|------|------------|
| Custom Queue Integration | Build adapter/bridge layer; evaluate Shotgun→NATS migration |
| Context Propagation | Implement as middleware in new platform |
| Custom Conditions | Port as custom task types or decision handlers |
| AssetMonitor | Rebuild as separate service or workflow patterns |
| Schema Migration | Data migration tooling; parallel database operation |

---

## Total Cost of Ownership (3-Year Projection)

*Assumptions: 100M workflow executions/year, 50 concurrent workflows, 3-person DevOps team at $150K/year*

| Platform | Year 1 | Year 2 | Year 3 | 3-Year Total |
|----------|--------|--------|--------|--------------|
| **Conductor OSS** | $200K (migration + infra) | $120K (infra + ops) | $120K | **$440K** |
| **Orkes** | $150K (migration + license) | $180K (license) | $180K | **$510K** |
| **Temporal Cloud** | $350K (migration + usage) | $200K (usage) | $200K | **$750K** |
| **AWS Step Functions** | $400K (migration) | $150K (usage) | $150K | **$700K** |
| **Current (d3sw)** | $150K (infra + ops) | $150K | $150K | **$450K** + tech debt |

*Note: Estimates are illustrative; actual costs depend on specific usage patterns and scale.*

---

## Conclusion

Based on the analysis, the recommended approach is:

1. **Short-term (0-6 months)**: Evaluate **Orkes Conductor** for a managed, low-risk migration path that preserves existing workflow investments.

2. **Medium-term alternative**: If budget constraints are primary, migrate to **Conductor OSS** and invest in porting customizations to the community fork.

3. **Long-term consideration**: If building a new platform from scratch or planning a multi-year modernization, **Temporal** offers the most robust foundation despite higher migration costs.

The final decision should weigh:
- Migration timeline requirements
- Team capacity for learning new paradigms
- Budget for licensing vs. operational costs
- Long-term platform strategy

---

## References and Resources

### Conductor
- [Conductor OSS GitHub](https://github.com/conductor-oss/conductor)
- [Orkes Platform](https://orkes.io)
- [Conductor OSS vs Orkes](https://www.orkes.io/platform/conductor-oss-vs-orkes)
- [Orkes Pricing](https://www.orkes.io/pricing)

### Temporal
- [Temporal.io](https://temporal.io/)
- [Temporal Cloud Pricing](https://temporal.io/pricing)
- [Temporal vs Conductor](https://orkes.io/compare/orkes-conductor-vs-temporal)

### AWS Step Functions
- [AWS Step Functions](https://aws.amazon.com/step-functions/)
- [Step Functions Pricing](https://aws.amazon.com/step-functions/pricing/)

### Camunda
- [Camunda Platform](https://camunda.com)
- [Camunda 8 Microservices Orchestration](https://camunda.com/solutions/microservices-orchestration/)

### Argo Workflows
- [Argo Workflows](https://argoproj.github.io/workflows/)
- [Argo GitHub](https://github.com/argoproj/argo-workflows)

### Azure Durable Functions
- [Azure Durable Functions Overview](https://learn.microsoft.com/en-us/azure/azure-functions/durable/durable-functions-overview)

### Apache Airflow
- [Apache Airflow](https://airflow.apache.org)
- [Airflow vs Temporal](https://www.zenml.io/blog/temporal-vs-airflow)

### General Comparisons
- [Top Open Source Workflow Orchestration Tools 2025](https://www.bytebase.com/blog/top-open-source-workflow-orchestration-tools/)
- [Workflow Orchestration Platforms Comparison 2025](https://procycons.com/en/blogs/workflow-orchestration-platforms-comparison-2025/)
- [Temporal Alternatives](https://akka.io/blog/temporal-alternatives)

---

*Document generated: January 2026*
*Last updated: January 2026*
*Author: Claude Code Analysis*
