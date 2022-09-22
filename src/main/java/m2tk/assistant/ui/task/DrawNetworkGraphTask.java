/*
 * Copyright (c) Ye Weibin. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m2tk.assistant.ui.task;

import guru.nidi.graphviz.attribute.GraphAttr;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import lombok.extern.slf4j.Slf4j;
import m2tk.assistant.Global;
import m2tk.assistant.dbi.DatabaseService;
import m2tk.assistant.dbi.entity.SIMultiplexEntity;
import m2tk.assistant.dbi.entity.SINetworkEntity;
import m2tk.assistant.dbi.entity.SIServiceEntity;
import m2tk.assistant.ui.dialog.NetworkGraphDialog;
import m2tk.assistant.ui.util.ComponentUtil;
import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.between;
import static guru.nidi.graphviz.model.Factory.port;

@Slf4j
public class DrawNetworkGraphTask extends Task<BufferedImage, Void>
{
    static class Context
    {
        Map<String, Node> nodes = new HashMap<>();
        Map<String, Object> siObjects = new HashMap<>();
        Map<String, List<String>> netMuxMap = new HashMap<>();
        Map<String, List<String>> muxSrvMap = new HashMap<>();
    }

    public DrawNetworkGraphTask(Application application)
    {
        super(application);
    }

    @Override
    protected BufferedImage doInBackground()
    {
        return query();
    }

    @Override
    protected void succeeded(BufferedImage result)
    {
        draw(result);
    }

    @Override
    protected void failed(Throwable cause)
    {
        log.warn("构建网络结构图时异常：{}", cause.getMessage(), cause);
        ComponentUtil.setWaitingMouseCursor(getContext().getFocusOwner().getRootPane(), false);
        JOptionPane.showMessageDialog(getContext().getFocusOwner(),
                                      "运行时异常，无法创建网络结构图",
                                      "请注意",
                                      JOptionPane.ERROR_MESSAGE);
    }


    private BufferedImage query()
    {
        DatabaseService databaseService = Global.getDatabaseService();
        long transactionId = Global.getCurrentTransactionId();
        List<SINetworkEntity> networks = databaseService.listNetworks(transactionId);
        List<SIMultiplexEntity> multiplexes = databaseService.listMultiplexes(transactionId);
        List<SIServiceEntity> services = databaseService.listServices(transactionId);

        Context context = new Context();
        createNodes(context, networks, multiplexes, services);
        List<LinkSource> linkSources = createNodeLinks(context);

        Graph graph = Factory.graph("NetworkGraph")
                             .directed()
                             .graphAttr().with(Rank.dir(LEFT_TO_RIGHT), Rank.sep(1.75), GraphAttr.splines(GraphAttr.SplineMode.LINE))
                             .nodeAttr().with("fontname", "SimSun")
                             .linkAttr().with("class", "link-class")
                             .with(linkSources);

        GraphvizCmdLineEngine engine = new GraphvizCmdLineEngine();
        engine.timeout(60, TimeUnit.SECONDS);
        Graphviz.useEngine(engine);

        return Graphviz.fromGraph(graph)
                       .engine(Engine.DOT)
                       .render(Format.PNG)
                       .toImage();
    }

    private void draw(BufferedImage image)
    {
        ComponentUtil.setWaitingMouseCursor(getContext().getFocusOwner().getRootPane(), false);
        NetworkGraphDialog dialog = new NetworkGraphDialog();
        dialog.showImage(image);
    }

    private void createNodes(Context context,
                             List<SINetworkEntity> networks,
                             List<SIMultiplexEntity> multiplexes,
                             List<SIServiceEntity> services)
    {
        int actualTransportStreamId = -1;
        for (SIServiceEntity service : services)
        {
            String srvKey = key(service);
            context.siObjects.put(srvKey, service);
            context.nodes.put(srvKey, createServiceNode(service));

            String muxKey = parentKey(service);
            context.muxSrvMap.computeIfAbsent(muxKey, k -> new ArrayList<>()).add(srvKey);

            if (service.isActualTransportStream())
                actualTransportStreamId = service.getTransportStreamId();
        }

        for (SIMultiplexEntity multiplex : multiplexes)
        {
            String muxKey = key(multiplex);
            context.siObjects.put(muxKey, multiplex);
            context.nodes.put(muxKey, createMultiplexNode(context, multiplex, actualTransportStreamId));

            String netKey = parentKey(multiplex);
            context.netMuxMap.computeIfAbsent(netKey, k -> new ArrayList<>()).add(muxKey);
        }

        for (SINetworkEntity network : networks)
        {
            String netKey = key(network);
            context.siObjects.put(netKey, network);
            context.nodes.put(netKey, createNetworkNode(context, network));
        }
    }

    private List<LinkSource> createNodeLinks(Context context)
    {
        List<LinkSource> linkSources = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : context.netMuxMap.entrySet())
        {
            String netKey = entry.getKey();
            List<String> muxKeys = entry.getValue();
            Node netNode = context.nodes.get(netKey);
            if (netNode == null)
            {
                netNode = createDummyNetNode(netKey, muxKeys);
                context.nodes.put(netKey, netNode);
            }
            for (String muxKey : muxKeys)
            {
                List<String> srvKeys = context.muxSrvMap.get(muxKey);
                Node muxNode = context.nodes.get(muxKey);
                if (muxNode == null)
                {
                    muxNode = createDummyMuxNode(muxKey, srvKeys);
                    context.nodes.put(muxKey, muxNode);
                }
                linkSources.add(netNode.link(between(port(muxKey + ":e"), muxNode.port("id:w"))));
            }
        }
        for (Map.Entry<String, List<String>> entry : context.muxSrvMap.entrySet())
        {
            String muxKey = entry.getKey();
            List<String> srvKeys = entry.getValue();
            Node muxNode = context.nodes.get(muxKey);
            if (muxNode == null)
            {
                muxNode = createDummyMuxNode(muxKey, srvKeys);
                context.nodes.put(muxKey, muxNode);
            }
            for (String srvKey : srvKeys)
            {
                // srvNode一定不为空，所以不用创建dummyNode
                Node srvNode = context.nodes.get(srvKey);
                linkSources.add(muxNode.link(between(port(srvKey + ":e"), srvNode.port("id:w"))));
            }
        }
        return linkSources;
    }

    private Node createServiceNode(SIServiceEntity service)
    {
        String label = Records.turn(Records.rec("id",
                                                String.format(" %s 业务%d",
                                                              service.isActualTransportStream() ? "[当前]" : "",
                                                              service.getServiceId())),
                                    Records.rec(String.format(" service_id：%d %n " +
                                                              " transport_stream_id：%d %n " +
                                                              " original_network_id：%d",
                                                              service.getServiceId(),
                                                              service.getTransportStreamId(),
                                                              service.getOriginalNetworkId())),
                                    Records.rec(String.format(" 业务类型：0x%02X（%s） %n " +
                                                              " 业务名称：%s %n " +
                                                              " 提供商：%s",
                                                              service.getServiceType(),
                                                              service.getServiceTypeName(),
                                                              (service.getServiceName() == null) ? "未命名业务" : service.getServiceName(),
                                                              (service.getServiceProvider() == null) ? "未知提供商" : service.getServiceProvider())),
                                    Records.rec(String.format(" 运行状态：%s %n " +
                                                              " 条件接收：%s %n " +
                                                              " 发送EIT_P/f：%s %n " +
                                                              " 发送EIT_Sch：%s",
                                                              service.getRunningStatus(),
                                                              service.isFreeCAMode() ? "否" : "是",
                                                              service.isPresentFollowingEITEnabled() ? "是" : "否",
                                                              service.isScheduleEITEnabled() ? "是" : "否")));
        return Factory.node(key(service))
                      .with("shape", "record")
                      .with(Records.label(label));
    }

    private Node createMultiplexNode(Context context, SIMultiplexEntity multiplex, int actualTransportStreamId)
    {
        List<String> records = new ArrayList<>();
        records.add(Records.rec("id",
                                String.format(" %s 传输流%d",
                                              (multiplex.getTransportStreamId() == actualTransportStreamId) ? "[当前]" : "",
                                              multiplex.getTransportStreamId())));
        records.add(Records.rec(String.format(" transport_stream_id：%d %n " +
                                              " original_network_id：%d",
                                              multiplex.getTransportStreamId(),
                                              multiplex.getOriginalNetworkId())));
        if (multiplex.getDeliverySystemType() != null)
            records.add(Records.rec(String.format(" 传输系统：%s", multiplex.getDeliverySystemType())));

        String muxKey = String.format("multiplex_%04x_%04x",
                                      multiplex.getTransportStreamId(),
                                      multiplex.getOriginalNetworkId());
        List<String> srvKeys = context.muxSrvMap.getOrDefault(muxKey, Collections.emptyList());
        srvKeys.sort(String::compareTo);
        for (String srvKey : srvKeys)
        {
            SIServiceEntity service = (SIServiceEntity) context.siObjects.get(srvKey);
            records.add(Records.rec(srvKey, String.format(" 业务%d", service.getServiceId())));
        }

        return Factory.node(key(multiplex))
                      .with("shape", "record")
                      .with(Records.of(records.toArray(new String[0])));
    }

    private Node createNetworkNode(Context context, SINetworkEntity network)
    {
        List<String> records = new ArrayList<>();
        records.add(Records.rec("id", String.format(" %s 网络%d",
                                                    network.isActualNetwork() ? "[当前]" : "",
                                                    network.getNetworkId())));
        records.add(Records.rec(String.format(" network_id：%d", network.getNetworkId())));
        if (network.getNetworkName() != null)
            records.add(Records.rec(String.format(" 网络名称：%s", network.getNetworkName())));

        String netKey = String.format("network_%04x", network.getNetworkId());
        List<String> muxKeys = context.netMuxMap.getOrDefault(netKey, Collections.emptyList());
        muxKeys.sort(String::compareTo);
        for (String muxKey : muxKeys)
        {
            SIMultiplexEntity multiplex = (SIMultiplexEntity) context.siObjects.get(muxKey);
            records.add(Records.rec(muxKey, String.format(" 传输流%d", multiplex.getTransportStreamId())));
        }

        return Factory.node(key(network))
                      .with("shape", "record")
                      .with(Records.of(records.toArray(new String[0])));
    }

    private Node createDummyMuxNode(String muxKey, List<String> srvKeys)
    {
        String[] parts = muxKey.split("_");
        int tsid = Integer.parseInt(parts[1], 16);
        int onid = Integer.parseInt(parts[2], 16);

        List<String> records = new ArrayList<>();
        records.add(Records.rec("id", String.format(" [X] 传输流%d", tsid)));
        records.add(Records.rec(String.format(" transport_stream_id：%d%n" +
                                              " original_network_id：%d",
                                              tsid,
                                              onid)));

        srvKeys.sort(String::compareTo);
        for (String srvKey : srvKeys)
        {
            parts = srvKey.split("_");
            int sid = Integer.parseInt(parts[1], 16);
            records.add(Records.rec(srvKey, String.format(" 业务%d", sid)));
        }

        return Factory.node(String.format("multiplex_%d", tsid))
                      .with("shape", "record")
                      .with(Records.of(records.toArray(new String[0])));
    }

    private Node createDummyNetNode(String netKey, List<String> muxKeys)
    {
        String[] parts = netKey.split("_");
        int nid = Integer.parseInt(parts[1], 16);

        List<String> records = new ArrayList<>();
        records.add(Records.rec("id", String.format(" [X] 网络%d", nid)));
        records.add(Records.rec(" network_id：" + nid));

        for (String muxKey : muxKeys)
        {
            parts = muxKey.split("_");
            int tsid = Integer.parseInt(parts[1], 16);
            records.add(Records.rec(muxKey, String.format(" 传输流%d", tsid)));
        }

        return Factory.node(String.format("network_%d", nid))
                      .with("shape", "record")
                      .with(Records.of(records.toArray(new String[0])));
    }

    private String key(SIServiceEntity service)
    {
        // 因为要在Node中作为ID使用，所以这里不能用“:”分隔，改用“_”分隔，下同。
        return String.format("service_%04x_%04x_%04x",
                             service.getServiceId(),
                             service.getTransportStreamId(),
                             service.getOriginalNetworkId());
    }

    private String key(SIMultiplexEntity multiplex)
    {
        return String.format("multiplex_%04x_%04x",
                             multiplex.getTransportStreamId(),
                             multiplex.getOriginalNetworkId());
    }

    private String key(SINetworkEntity network)
    {
        return String.format("network_%04x", network.getNetworkId());
    }

    private String parentKey(SIServiceEntity service)
    {
        return String.format("multiplex_%04x_%04x",
                             service.getTransportStreamId(),
                             service.getOriginalNetworkId());
    }

    private String parentKey(SIMultiplexEntity multiplex)
    {
        return String.format("network_%04x", multiplex.getNetworkId());
    }
}
