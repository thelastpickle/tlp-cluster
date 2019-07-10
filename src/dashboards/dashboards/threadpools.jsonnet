local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local singlestat = grafana.singlestat;
local prometheus = grafana.prometheus;
local template = grafana.template;

dashboard.new(
    "Threadpools",
    schemaVersion=17,
    tags=["cassandra"]
).addPanel(
    singlestat.new(
        "Blocked",
        format="s",
        datasource="Prometheus",
        span=2,
        valueName="current"
    ).addTarget(
        prometheus.target(
            "now-10m"
    )
    ), gridPos={
                  x: 0,
                  y: 0,
                  w: 24,
                  h: 3,
                })