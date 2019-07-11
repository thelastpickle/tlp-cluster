local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local prometheus = grafana.prometheus;
local template = grafana.template;

dashboard.new(
  'TLP - Cassandra Overview',
  schemaVersion=18,
  refresh='1m',
  time_from='now-3h',
  editable=true,
  tags=['Cassandra', 'TLP', 'tlp', 'C*', 'The Last Pickle', 'Overview'],
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
    'environment',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate, environment)',
    label='Environment',
    refresh='time',
  )
)
.addTemplate(
  template.new(
    'cluster',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment"}, cluster)',
    label='Cluster',
    refresh='time',
  )
)
.addTemplate(
  template.new(
    'datacenter',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster"}, datacenter)',
    label='Datacenter',
    refresh='time',
    includeAll=true,
    multi=true,
  )
)
.addTemplate(
  template.new(
    'rack',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter"}, rack)',
    label='Rack',
    refresh='time',
    includeAll=true,
    multi=true,
  )
)
.addTemplate(
  template.new(
    'node',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack"}, node)',
    label='Node',
    refresh='time',
    current='all',
    includeAll=true,
    multi=true,
  )
)
.addPanel(
  graphPanel.new(
    'Request Rates',
    description='Coordinator level read and write count - Max of each 1m rate for each operation type',
    format='rps',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'sum by (operation)(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  ), gridPos={
    x: 0,
    y: 0,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Dropped Messages',
    description='Dropped messages rate summed by message type',
    format='short',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'sum by (message_type) (org_apache_cassandra_metrics_droppedmessage_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  ), gridPos={
    x: 8,
    y: 0,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Pending Messages',
    description='Pending threads rate summed by pool',
    format='short',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'sum by (pool) (org_apache_cassandra_metrics_threadpools_value{name="PendingTasks", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  )
  , gridPos={
    x: 16,
    y: 0,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Read Latency',
    description='Read latency on p99 for coordinated reads',
    format='µs',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'org_apache_cassandra_metrics_clientrequest_99thpercentile{operation="read", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}',
    )
  ), gridPos={
    x: 0,
    y: 6,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Write Latency',
    description='Write latency on p99 for coordinated reads',
    format='µs',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'org_apache_cassandra_metrics_clientrequest_99thpercentile{operation="write", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}',
    )
  ), gridPos={
    x: 8,
    y: 6,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Other Latencies',
    description='Other latencies on p99 for coordinated requests',
    format='µs',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'max by (operation) (org_apache_cassandra_metrics_clientrequest_99thpercentile{operation!~"write|read", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  ), gridPos={
    x: 16,
    y: 6,
    w: 8,
    h: 6,
  }
)

.addPanel(
  graphPanel.new(
    'SSTable Count',
    description='SSTable Count Max and Average per node and table',
    format='short',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'max by (table) (org_apache_cassandra_metrics_table_count{name="SSTablesPerReadHistogram", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  )
  .addTarget(
    prometheus.target(
      'avg by (table) (org_apache_cassandra_metrics_table_count{name="SSTablesPerReadHistogram", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  ), gridPos={
    x: 0,
    y: 12,
    w: 8,
    h: 6,
  }
)
.addPanel(
  graphPanel.new(
    'Pending Compactions',
    description='Pending compactions per node',
    format='short',
    datasource='Prometheus',
    transparent=true,
    fill=0,
    legend_show=false,
  )
  .addTarget(
    prometheus.target(
      'sum by (table) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
    )
  ), gridPos={
    x: 8,
    y: 12,
    w: 8,
    h: 6,
  }
)
