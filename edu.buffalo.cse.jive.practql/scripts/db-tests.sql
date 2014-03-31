set search_path to jiveyyyymmddhhnnss,public;

select * from event;
select * from event_ecatch;
select * from event natural inner join event_ethrow;
select * from event natural inner join event_fread;
select * from event_mcall;
select * from event_mexit;
select * from event_mret;

select * from value order by kind

select * from value_cref order by vid

select * from value natural inner join value_om_mkeyref order by 3 desc

select * from value natural inner join value_om_mcref order by 3 desc

select * from value natural inner join value_om_resolved order by 3 desc

select * from value_thread order by 1

select * from value_file order by 1

select vlid, name, number from value_line natural inner join value_file order by 2, 3

select * from value_file natural join value_line 
select * from value_file
select * from node where origin = 'NO_JDI' AND kind = 'NK_CLASS' order by nid
select kind, count(*) from node group by kind
select * from node natural inner join node_type order by nid
select * from node natural inner join node_data order by nid
select * from node natural inner join node_method order by origin, nid

select * from noderef_method order by refid
select * from noderef_type order by refid
select * from contour_member
select * from node_type
select * from value

select c.kind, cn.name, c.ordinal, cm.kind, cm.name, v.value, cmth.name, cmth.ordinal
  from contour c inner join node cn on c.schemaid = cn.nid
  inner join (contour_member cm inner join node n on cm.schemaid = n.nid) cm on (c.contourId = cm.contourId) 
  left join value v on v.vid = cm.valueId
  left join value_cref vm on vm.vid = cm.valueId
  left join (contour c inner join node cn on c.schemaid = cn.nid) cmth on vm.contourid = cmth.contourId
order by 2,3,cm.nid

select cm.contourId, nd.name as member, cm.fromTime, cm.toTime, COALESCE(v.value, nr.name || ':' || cr.ordinal::text) as value
  from contour_member cm 
  inner join node nd on cm.schemaId = nd.nid
  left join value v on v.vid = cm.valueId 
  left join value_cref vr on vr.vid = cm.valueId
  left join contour cr on vr.contourId = cr.contourId
  left join node nr on cr.schemaId = nr.nid
order by 1, 2, 3, 4

select c.contourId, 
  CASE WHEN c.kind = 'CK_STATIC'::ContourKind THEN nd.name 
  ELSE COALESCE(nd.name || ':' || c.ordinal::text) END as value,
  CASE WHEN c.kind = 'CK_METHOD'::ContourKind THEN 'method' 
       WHEN c.kind = 'CK_STATIC'::ContourKind THEN 'static' 
  ELSE 'instance' END AS kind,
  c.fromTime, c.toTime
  from contour c 
  inner join node nd on c.schemaId = nd.nid
order by 4,1

select 'NM_RPDL'::NodeModifier = ANY (modifiers) from node natural join node_data
select * from node_data natural join node
select nt.key, nt.refId, lnt.key, lnrt.key
from (node_type_interface
      natural join node_type) nt
     left join node_type lnt on nt.refid = lnt.nid
     left join noderef_type lnrt on nt.refid = lnrt.refid

select * from node_method_exception nme natural join node_method nm inner join noderef_type nt on nme.refid = nt.refid

select (select count(*) from node) - (select count(*) from node_type) - (select count(*) from node_method) - (select count(*) from node_data)
select kind, count(*) from node group by kind

select 26000.0 / 1.6