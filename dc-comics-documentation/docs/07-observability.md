---
sidebar_position: 7
---

# Observability

Observability stack is composed of these primitives:

* Logging 
* Healthcheck(s): this is the capacity to test through HTTP the server state, mainly used by Kubernetes to check if the application is ready (can get traffic) and if it is in a broken state (pod should be killed and restarted for example).
* Metrics: often coupled to opentelemetry, it enables to collect metrics (think time series) about your application. It can be technical (CPU usage for example) or business (number of queries, ...).
* Tracing: this is the ability to trace a business request (a _trace_) end to end through all the system. Main collectors/UI are Jaegger. 

## Logging
