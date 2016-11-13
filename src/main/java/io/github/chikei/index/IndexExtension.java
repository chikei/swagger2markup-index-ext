package io.github.chikei.index;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.markup.builder.MarkupDocBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.github.swagger2markup.markup.builder.MarkupTableColumn;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.OverviewDocumentExtension;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class IndexExtension extends OverviewDocumentExtension {
    private static final String EXTENSION_ID = "pathIndex";
    private Swagger model = null;

    static final Ordering<HttpMethod> OPERATION_METHOD_NATURAL_ORDERING = Ordering
            .explicit(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.HEAD, HttpMethod.OPTIONS);

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        model = globalContext.getSwagger();
    }

    @Override
    public void apply(Context context) {
        MarkupDocBuilder builder = context.getMarkupDocBuilder();
        Position position = context.getPosition();

        if (position.equals(Position.DOCUMENT_END)) {
            builder.sectionTitleLevel1("Path Index");
            buildTable(builder, operations(model), getPathOperations(model));
        }
    }

    private Set<HttpMethod> operations(Swagger model) {
        Set<HttpMethod> ops = new TreeSet<>(OPERATION_METHOD_NATURAL_ORDERING);
        for (Map.Entry<String, Path> path : model.getPaths().entrySet()) {
            for (Map.Entry<HttpMethod, Operation> op : path.getValue().getOperationMap().entrySet()) {
                ops.add(op.getKey());
            }
        }
        return ops;
    }

    private Map<String, Map<HttpMethod, PathOperation>> getPathOperations(Swagger model) {
        Map<String, Path> paths = model.getPaths();
        return paths.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        p -> p.getValue().getOperationMap().entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                        e -> new PathOperation(e.getKey(), p.getKey(), e.getValue())))));
    }

    private void buildTable(MarkupDocBuilder docBuilder, Set<HttpMethod> methods, Map<String, Map<HttpMethod, PathOperation>> paths) {
        ArrayList<MarkupTableColumn> cols = new ArrayList<>();
        cols.add(new MarkupTableColumn("Path").withWidthRatio(5).withHeaderColumn(false).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^5"));
        for(HttpMethod m: methods){
            cols.add(new MarkupTableColumn(m.name()).withWidthRatio(1).withHeaderColumn(false).withMarkupSpecifiers(MarkupLanguage.ASCIIDOC, ".^1"));
        }

        List<List<String>> cells = new ArrayList<>();

        SortedMap<String, Map<HttpMethod, PathOperation>> sortedPaths = new TreeMap<>();
        sortedPaths.putAll(paths);
        sortedPaths.forEach((path, ops) -> {
            ArrayList<String> content = new ArrayList<>();
            content.add(path);
            methods.forEach(method -> {
                if(ops.containsKey(method)){
                    content.add(displayOperation(docBuilder, ops.get(method)));
                } else content.add("");
            });
            cells.add(content);
        });
        docBuilder.tableWithColumnSpecs(cols, cells);
    }

    private String displayOperation(MarkupDocBuilder docBuilder, PathOperation operation) {
        return docBuilder.copy(false).crossReference(operation.getId(), operation.getMethod().name()).toString();
    }
}
