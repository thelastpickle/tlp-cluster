local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local prometheus = grafana.prometheus;
local template = grafana.template;

dashboard.new(
  'TLP - Cassandra Overview',
  schemaVersion=14,
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
.addRow(
  row.new(title='Nodes Status')
  .addPanel(
    tablePanel.new(
      'Nodes Up/Down',
      description='Nodes being up or down - For now uses "up" metric, that only says if we could scrape metrics or not. To be improved',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "Up?",
          "colorMode": "row",
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 0,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0",
            "1"
          ],
          "type": "number",
          "unit": "short"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": false
      }
    )
    .addTarget(
      prometheus.target(
        'min by (node) (up{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{node}}',
        instant=true
      )
    )
  )
  .addPanel(
    tablePanel.new(
      'Disk Space Usage',
      description='Disks space used ordered (fullest disks first)',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "% Disk Space Used",
          "colorMode": "row",
          "colors": [
            "rgba(50, 172, 45, 0.97)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(245, 54, 54, 0.9)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0.5",
            "0.75",
          ],
          "type": "number",
          "unit": "percentunit"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": true
      }
    )
    .addTarget(
      prometheus.target(
        '(1-(node_filesystem_avail_bytes/node_filesystem_size_bytes))',
        legendFormat='{{node}} --> {{mountpoint}}',
        instant=true
      )
    )
  )
)
.addRow(
  row.new(title='Client Requests',)
  .addPanel(
    graphPanel.new(
      'Request Rates',
      description='Coordinator level read and write count - Max of each 1m rate for each operation type',
      format='rps',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope=~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{scope}} Request Rate',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Request Rates per Consistency Level',
      description='Coordinator level read and write count - Max of each 1m rate for each operation type',
      format='rps',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope!~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{scope}} Request Rate',
      )
    )
  )
  .addPanel(
    singleStatPanel.new(
      'Read / Write Distribution',
      description='Part of writes in the total of standard requests (Reads+Writes). CAS, Views, ... operations are ignored.',
      format='percentunit',
      datasource='Prometheus',
      transparent=true,
      postfix=' Reads',
      postfixFontSize='30%',
      valueFontSize='30%',
      valueName="current",
      decimals=2,
      thresholds='0.25,0.5,0.75',
      timeFrom='1m',
      colors=[
        "#DEB6F2",
        "#CA95E5",
        "#8F3BB8"
      ],
      gaugeShow=true,
      gaugeMinValue=0,
      gaugeMaxValue=1,
      gaugeThresholdLabels=true,
      gaugeThresholdMarkers=false,
      sparklineFillColor='rgba(31, 118, 189, 0.18)',
      sparklineFull=false,
      sparklineLineColor='#FFB357',
      sparklineShow=true
    )
    .addTarget(
      prometheus.target(
        'sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) / (sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) + sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}))',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Read Latency',
      description='Read latency maximum for coordinated reads',
      format='µs',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_75thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_95thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Write Latency',
      description='Write latency maximum for coordinated reads',
      format='µs',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_75thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_95thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (node) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
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
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (operation) (org_apache_cassandra_metrics_clientrequest_99thpercentile{operation!~"write|read", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
)
.addRow(
  row.new(title='Cassandra Internals',)
 .addPanel(
    graphPanel.new(
      'Dropped Messages',
      description='Dropped messages rate summed by message type',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (message_type) (org_apache_cassandra_metrics_droppedmessage_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    ), gridPos={w: 6, h: 6, x: 0,}
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
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (pool) (org_apache_cassandra_metrics_threadpools_value{name="PendingTasks", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
    , gridPos={w: 6, h: 6, x: 6,}
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
      shared_tooltip=false,
      min=0,
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
    ), gridPos={w: 6, h: 6, x: 12,}
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
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (table) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    ), gridPos={w: 6, h: 6, x: 18,}
  )
)
.addRow(
  row.new(title='Hardware / Operating System',)
 .addPanel(
    graphPanel.new(
      'CPU',
      description='CPU Average per CPU mode and node',
      format='percent',
      datasource='Prometheus',
      transparent=true,
      fill=1,
      legend_show=false,
      shared_tooltip=false,
      stack=true,
      percentage=true,
    )
    .addTarget(
      prometheus.target(
        'avg by (mode) (rate(node_cpu_seconds_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
      )
    ), gridPos={w: 8, h: 6, x: 0,}
  )
  .addPanel(
    graphPanel.new(
      'Unix Load',
      description='Unix load per node',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'node_load1{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}',
      )
    ), gridPos={w: 8, h: 6, x: 8,}
  )
  .addPanel(
    graphPanel.new(
      'Network I/O',
      description='Network In and Out per node',
      format='bytes',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (node) (rate(node_network_transmit_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
      )
    )
    .addTarget(
      prometheus.target(
        'sum by (node) (rate(node_network_receive_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
      )
    ), gridPos={w: 8, h: 6, x: 16,}
  )
)
.addRow(
  row.new(title='JVM / Garbage Collection',)
 .addPanel(
    graphPanel.new(
      'Garbage Collection',
      description='Garbage Collection duration',
      format='s',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=false,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (gc) (rate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
      )
    ), gridPos={w: 8, h: 6, x: 0,}
  )
)
