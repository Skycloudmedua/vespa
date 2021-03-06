// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.restapi.v2;

import com.yahoo.container.jdisc.HttpRequest;
import com.yahoo.container.jdisc.HttpResponse;
import com.yahoo.slime.Cursor;
import com.yahoo.slime.Slime;
import com.yahoo.vespa.config.SlimeUtils;
import com.yahoo.vespa.hosted.provision.Node;
import com.yahoo.vespa.hosted.provision.NodeRepository;
import com.yahoo.vespa.hosted.provision.node.NodeAcl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * @author mpolden
 */
public class NodeAclResponse extends HttpResponse {

    private static final String CHILDREN_REQUEST_PROPERTY = "children";

    private final NodeRepository nodeRepository;
    private final Slime slime;
    private final boolean aclsForChildren;

    public NodeAclResponse(HttpRequest request, NodeRepository nodeRepository) {
        super(200);
        this.nodeRepository = nodeRepository;
        this.slime = new Slime();
        this.aclsForChildren = request.getBooleanProperty(CHILDREN_REQUEST_PROPERTY);

        final Cursor root = slime.setObject();
        final String hostname = baseName(request.getUri().getPath());
        toSlime(hostname, root);
    }

    private void toSlime(String hostname, Cursor object) {
        Node node = nodeRepository.getNode(hostname)
                .orElseGet(() -> nodeRepository.getConfigNode(hostname)
                        .orElseThrow(() -> new NotFoundException("No node with hostname '" + hostname + "'")));

        List<NodeAcl> acls = nodeRepository.getNodeAcls(node, aclsForChildren);

        Cursor trustedNodesArray = object.setArray("trustedNodes");
        acls.forEach(nodeAcl -> toSlime(nodeAcl, trustedNodesArray));

        Cursor trustedNetworksArray = object.setArray("trustedNetworks");
        acls.forEach(nodeAcl -> toSlime(nodeAcl.trustedNetworks(), nodeAcl.node(), trustedNetworksArray));

        Cursor trustedPortsArray = object.setArray("trustedPorts");
        acls.forEach(nodeAcl -> toSlime(nodeAcl.trustedPorts(), nodeAcl, trustedPortsArray));
    }

    private void toSlime(NodeAcl nodeAcl, Cursor array) {
        nodeAcl.trustedNodes().forEach(node -> node.ipAddresses().forEach(ipAddress -> {
            Cursor object = array.addObject();
            object.setString("hostname", node.hostname());
            object.setString("type", node.type().name());
            object.setString("ipAddress", ipAddress);
            object.setString("trustedBy", nodeAcl.node().hostname());
        }));
    }

    private void toSlime(Set<String> trustedNetworks, Node trustedby, Cursor array) {
        trustedNetworks.forEach(network -> {
            Cursor object = array.addObject();
            object.setString("network", network);
            object.setString("trustedBy", trustedby.hostname());
        });
    }

    private void toSlime(Set<Integer> trustedPorts, NodeAcl trustedBy, Cursor array) {
        trustedPorts.forEach(port -> {
            Cursor object = array.addObject();
            object.setLong("port", port);
            object.setString("trustedBy", trustedBy.node().hostname());
        });
    }

    @Override
    public void render(OutputStream outputStream) throws IOException {
        outputStream.write(SlimeUtils.toJsonBytes(slime));
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    private static String baseName(String path) {
        return new File(path).getName();
    }
}
