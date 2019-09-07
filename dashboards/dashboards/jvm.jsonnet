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
  'JVM',
  schemaVersion=14,
  refresh='1m',
  time_from='now-15m',
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
    row.new("Regions")
    .addPanel(
        graphPanel.new(
            "Eden Usage",
            description="Eden Usage",
            datasource='$PROMETHEUS_DS'
        )
        .addTarget(
            prometheus.target(
                'jvm_memory_pool_bytes_used{pool="Par Eden Space"} / 2^20',
                legendFormat="{{instance}}",
            )
        )
    )
    .addPanel(
       graphPanel.new(
           "CMS Old Gen Usage",
           description="CMS Old Gen Usage",
           datasource='$PROMETHEUS_DS'
       )
       .addTarget(
           prometheus.target(
               'jvm_memory_pool_bytes_used{pool="CMS Old Gen"} / 2^20',
               legendFormat="{{instance}}",
           )
       )
    )
)