FROM mysql:8.4

USER root
RUN microdnf install -y iproute jq python3 ca-certificates && microdnf clean all

WORKDIR /opt/plan086
COPY scripts/ci/verify-demo-private-network.sh /opt/plan086/verify-demo-private-network.sh
COPY scripts/ci/verify-demo-mysql-readonly.sh /opt/plan086/verify-demo-mysql-readonly.sh
COPY scripts/ci/demo-seed-preflight.sh /opt/plan086/demo-seed-preflight.sh
COPY scripts/ci/demo-seed-postflight.sh /opt/plan086/demo-seed-postflight.sh
COPY scripts/ci/run-demo-db-job.sh /opt/plan086/run-demo-db-job.sh
RUN chmod 0555 /opt/plan086/*.sh

ENTRYPOINT ["/bin/bash", "/opt/plan086/run-demo-db-job.sh"]
