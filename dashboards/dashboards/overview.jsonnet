local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local textPanel = grafana.text;
local prometheus = grafana.prometheus;
local template = grafana.template;

local fillMinMaxSeriesOverrides = {
    "alias": "/.*max.*/",
    "fillBelowTo": "min",
    "lines": false
};
local removeMinlineSeriesOverrides = {
    "alias": "/.*min.*/",
    "lines": false
};

dashboard.new(
  'Cassandra Overview',
  schemaVersion=14,
  refresh='1m',
  time_from='now-15m',
  editable=true,
  tags=['Cassandra', 'Overview'],
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
  row.new(title='Cluster Requests (Coordinator Perspective)',)
  .addPanel(
    graphPanel.new(
      'Request Throughputs',
      description='Total Requests Per Cluster, by Request Type',
      format='rps',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope=~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name=~"Latency|ViewWriteLatency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{scope}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Error throughputs',
      description='Total Timeouts, Failures, Unavailable Rates for each cluster',
      format='rps',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (name, environment, cluster) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope=~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name!~"Latency|ViewWriteLatency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{name}}',
      )
    )
  )
  .addPanel(
    singleStatPanel.new(
      'Read / Write Distribution',
      description='Part of reads in the total of standard requests (Reads+Writes). CAS, Views, ... operations are ignored.',
      format='percentunit',
      datasource='$PROMETHEUS_DS',
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
      sparklineShow=false
    )
    .addTarget(
      prometheus.target(
        'sum by (scope, environment, cluster)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) / ignoring (scope) (sum by (scope, environment, cluster)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) + ignoring (scope) sum by (scope, environment, cluster)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}))',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Read Latency (99th percentile)',
      description='p99 Read latency maximum for coordinated reads',
      format='µs',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Write Latency (99th Percentile)',
      description='p99 Write latency maximum for coordinated reads',
      format='µs',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Other Latencies',
      description='Other p99 latencies for coordinated requests',
      format='µs',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        # In scope!~"Write|Read|.*-.*", we want to exclude charts above and all the per-consistency_level info like "Read-LOCAL_ONE"
        'max by (scope, environment, cluster) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope!~"Write|Read|.*-.*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max {{scope}}'
      )
    )
  )
)
.addRow(
  row.new(title='Data Status')
  .addPanel(
    tablePanel.new(
      'Disk Space Usage',
      description='Disk space used ordered (fullest disks first)',
      datasource='$PROMETHEUS_DS',
      transform='timeseries_aggregations',
      transparent=true,
      styles=[
        {
          "alias": "Node --> Mounting Point",
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
        'max by (environment, cluster, datacenter, rack, node, mountpoint)(1-(node_filesystem_avail_bytes{mountpoint!~".*lxcfs.*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        / node_filesystem_size_bytes{mountpoint!~".*lxcfs.*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}))',
        legendFormat='{{cluster}}-{{node}} --> {{mountpoint}}',
        instant=true
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Total Data Size',
      description='Total sizes of the data on distinct nodes',
      format='bytes',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (name, environment, cluster)
        (org_apache_cassandra_metrics_table_value{name=~"LiveDiskSpaceUsed|TotalDiskSpaceUsed", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"} )',
        legendFormat='{{name}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'SSTable Count',
      description='SSTable Count Max and Average per table',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (keyspace, scope, environment, cluster) (org_apache_cassandra_metrics_table_value{name="LiveSSTableCount", keyspace=~".+", scope=~".+", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Table {{keyspace}}.{{scope}}',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster) (org_apache_cassandra_metrics_table_value{name="LiveSSTableCount", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Max in cluster {{cluster}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Pending Compactions',
      description='Maximum pending compactions on any node in the cluster',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
      bars=false,
      lines=true,
      stack=false,
      decimals=0,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (environment, cluster) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Pending Compactions per Table',
      description='Maximum pending compactions per table',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
      bars=false,
      lines=true,
      stack=true,
      decimals=0,
    )
    .addTarget(
      prometheus.target(
        'Max by (keyspace, scope, environment, cluster) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", keyspace=~".+", scope=~".+", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max for {{keyspace}}.{{scope}}',
      )
    )
  )
)
.addRow(
  row.new(title='Cassandra Internals',)
  .addPanel(
    graphPanel.new(
      'Cluster Wide Pending Messages',
      description='Cluster Wide Pending threads rate, by Thread Pool',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope, environment) (org_apache_cassandra_metrics_threadpools_value{name="PendingTasks", scope!~"RPC-Thread", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{cluster}}/{{scope}}',
      )
    )
  )
 .addPanel(
    graphPanel.new(
      'Dropped Messages',
      description='Dropped messages rate summed by message type and cluster',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope, environment, cluster) (org_apache_cassandra_metrics_droppedmessage_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{scope}}',
      )
    )
  )
  .addPanel(
     graphPanel.new(
       'Read Repairs',
       description='Read repair rate summed per cluster',
       format='short',
       datasource='$PROMETHEUS_DS',
       transparent=true,
       fill=0,
       legend_show=true,
       legend_values=true,
       legend_current=true,
       legend_alignAsTable=true,
       legend_sort='current',
       legend_sortDesc=true,
       shared_tooltip=false,
       min=0,
     )
     .addTarget(
       prometheus.target(
         'sum by (environment, cluster) (org_apache_cassandra_metrics_threadpools_value{name="ActiveTasks", scope="ReadRepairStage", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
         legendFormat='count',
       )
     )
   )
  .addPanel(
     graphPanel.new(
       'Hinted Handoff',
       description='Sum of hints being handed off per cluster.',
       format='short',
       datasource='$PROMETHEUS_DS',
       transparent=true,
       fill=0,
       legend_show=true,
       legend_values=true,
       legend_current=true,
       legend_alignAsTable=true,
       legend_sort='current',
       legend_sortDesc=true,
       shared_tooltip=false,
       min=0,
     )
     .addTarget(
       prometheus.target(
         'sum by (environment, cluster) (org_apache_cassandra_db_storageproxy_hintsinprogress{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
         legendFormat='count',
       )
     )
   )
)
.addRow(
  row.new(title='Hardware / Operating System',)
 .addPanel(
    graphPanel.new(
      'CPU Utilization',
      description='Maximum CPU utilisation (max 100%)',
      format='percent',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      percentage=true,
      decimals=1,
      min=0,
      max=105,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster) (100 * (1 - avg by (mode, environment, cluster, datacenter, rack, node) (irate(node_cpu_seconds_total{mode="idle", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))))',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster) (100 * (1 - avg by (mode, environment, cluster, datacenter, rack, node) (irate(node_cpu_seconds_total{mode="idle", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))))',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (environment, cluster) (100 * (1 - avg by (mode, environment, cluster, datacenter, rack, node) (irate(node_cpu_seconds_total{mode="idle", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))))',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Unix Load (1m rate)',
      description='Max Unix load on a node for a cluster',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster) (node_load1{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster) (node_load1{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (environment, cluster) (node_load1{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Memory Utilisation',
      description='Maximum Memory allocated per usage (worst node) - excludes caches, buffers, etc',
      format='bytes',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      fill=1,
      linewidth=2,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster)
        (node_memory_MemTotal_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_MemFree_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Buffers_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Cached_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_SwapCached_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Slab_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_PageTables_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_VmallocUsed_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        )',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster)
        (node_memory_MemTotal_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='min',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Disk IO Wait',
      description='Equivalent to r_await and w_await from `iostat -x` - Building help, see: https://www.robustperception.io/mapping-iostat-to-the-node-exporters-node_disk_-metrics',
      format='percent',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster)
        (100 * irate(node_disk_read_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
        / irate(node_disk_reads_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))',
        legendFormat='max r_await',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster)
        (100 * irate(node_disk_write_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
        / irate(node_disk_writes_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))',
        legendFormat='max w_await',
      )
    )
    /* .addTarget(
    # TODO Not working
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (100 *
          (irate(node_disk_read_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
          + irate(node_disk_write_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))
        ) / (
          (irate(node_disk_reads_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
          + irate(node_disk_writes_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))
        )',
        legendFormat='Max await in: {{cluster}}',
      )
    ) */
  )
  .addPanel(
    graphPanel.new(
      'Network I/O',
      description='Network In and Out per cluster',
      format='bytes',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=1,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      bars=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster) (irate(node_network_transmit_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='outgoing',
      )
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster) (irate(node_network_receive_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='incoming',
      )
    )
    .addSeriesOverride({
          "alias": "incoming",
          "transform": "negative-Y"
    })

  )
)
.addRow(
  row.new(title='JVM / Garbage Collection',)
  .addPanel(
    graphPanel.new(
      'Garbage Collection rate (% time spent doing GC)',
      description='Percentage of the time node is doing GC per cluster thus were Cassandra can\'t use resources',
      format='percentunit',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      decimals=2,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster) (sum by (environment, cluster, datacenter, rack, node) (irate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m])))',
        legendFormat='max',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster) (sum by (environment, cluster, datacenter, rack, node) (irate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m])))',
        legendFormat='min',
      )
    )
    .addTarget(
      prometheus.target(
        'avg by (environment, cluster) (sum by (environment, cluster, datacenter, rack, node) (irate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m])))',
        legendFormat='avg',
      )
    )
    .addSeriesOverride(fillMinMaxSeriesOverrides)
    .addSeriesOverride(removeMinlineSeriesOverrides)
  )
  .addPanel(
    graphPanel.new(
      'Garbage Collection Time',
      description='Max garbage collection duration',
      format='s',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (gc, environment, cluster) (irate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='Maximum {{gc}} duration (ms/s)',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Garbage Collection Count',
      description='Garbage Collection Count',
      format='short',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      fill=0,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (gc, environment, cluster) (irate(jvm_gc_collection_seconds_count{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='Maximum {{gc}} count per minute',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'JVM Heap Memory Utilisation',
      description='Maximum JVM Heap Memory size (worst node)',
      format='bytes',
      datasource='$PROMETHEUS_DS',
      transparent=true,
      legend_show=true,
      legend_values=true,
      legend_current=true,
      legend_alignAsTable=true,
      legend_sort='current',
      legend_sortDesc=true,
      shared_tooltip=false,
      fill=1,
      linewidth=2,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster)
        (jvm_memory_bytes_used{area="heap", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Max heap size used',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster)
        (jvm_memory_bytes_max{area="heap", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Min heap memory available',
      )
    )
  )
)
