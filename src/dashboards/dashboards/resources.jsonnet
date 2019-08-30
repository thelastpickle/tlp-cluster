local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local textPanel = grafana.text;
local prometheus = grafana.prometheus;
local template = grafana.template;



dashboard.new(
  'System Resources',
  schemaVersion=14,
  refresh='1m',
  time_from='now-3h',
  editable=true,
  tags=['Cassandra', 'Resources', 'Network', 'Disk'],
)
.addTemplate(
  grafana.template.datasource(
    'PROMETHEUS_DS',
    'prometheus',
    'Prometheus',
    hide='label',
  )
)
.addTemplate(
  template.new(
    'node',
    '$PROMETHEUS_DS',
    'label_values(node_cpu_seconds_total{cpu="0", mode="user", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack"}, node)',
    label='Node',
    refresh='time',
    current='all',
    includeAll=true,
    multi=true,
  )
)
.addRow(
    row.new("Disk")
    .addPanel(
        graphPanel.new(
            "Disk Throughput",
            description="Disk Throughput",
            datasource='$PROMETHEUS_DS'
        )
        .addTarget(
            prometheus.target(
                'sum(rate(mutations_count{job="stress"}[15s])) by (instance)',
                legendFormat="{{instance}}"
            )
        )
    )
    .addPanel(
        graphPanel.new(
            "CPU",
            description="CPU Usage",
            datasource='$PROMETHEUS_DS',
        )
        .addTarget(
            prometheus.target(
                '100-(100 * avg(rate(node_cpu_seconds_total{mode="idle"}[30s])) by (instance))',
                legendFormat="{{instance}}"
            )
        )
    )
)