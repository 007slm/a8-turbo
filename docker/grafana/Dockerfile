FROM grafana/grafana:9.5.2
ENV GF_INSTALL_PLUGINS=redis-datasource

COPY grafana.ini /etc/grafana/grafana.ini
COPY grafana.db /var/lib/grafana/grafana.db

# 创建 dashboards 目录并复制 NATS dashboard
RUN mkdir -p /var/lib/grafana/dashboards
COPY nats-dashboard.json /var/lib/grafana/dashboards/nats-dashboard.json
COPY dashboards.yml /etc/grafana/provisioning/dashboards/dashboards.yml

USER root
RUN chown grafana /etc/grafana/grafana.ini
RUN chown grafana /var/lib/grafana/grafana.db
RUN chown -R grafana /var/lib/grafana/dashboards