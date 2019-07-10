local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local singlestat = grafana.singlestat;
local prometheus = grafana.prometheus;
local template = grafana.template;

local blockedTasks =
    singlestat.new(
     "Blocked",
     format="s",
     datasource="Prometheus",
     span=2,
     valueName="current"
    ).addTarget(
       prometheus.target(
        "cassandra_threadpool_request"
       )
      );


dashboard.new(
    "Threadpools",
    schemaVersion=18,
    tags=["cassandra"]
).addTemplate(
    template.datasource(
      'PROMETHEUS_DS',
      'prometheus',
      'Prometheus')
      ).addPanels(
      [
        blockedTasks {gridPos: {h:4, w:4, y:0}  }

      ])
