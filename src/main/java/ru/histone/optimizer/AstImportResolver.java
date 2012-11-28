/**
 *    Copyright 2012 MegaFon
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.histone.optimizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.histone.parser.Parser;
import ru.histone.resourceloaders.ResourceLoader;

public class AstImportResolver {
    private static final Logger log = LoggerFactory.getLogger(AstImportResolver.class);

    private ResourceLoader resourceLoader;
    private Parser parser;

    public AstImportResolver(Parser parser, ResourceLoader resourceLoader) {
        this.parser = parser;
        this.resourceLoader = resourceLoader;
    }

/*    public ArrayNode resolve(ArrayNode ast) throws HistoneException {
        ImportResolverContext context = new ImportResolverContext();
        return resolveInternal(ast, context);
    }

    private ArrayNode resolveInternal(ArrayNode ast, ImportResolverContext context) throws HistoneException {
        ArrayNode result = new ArrayNode();

        for (JsonNode element : ast) {
            JsonNode node = scanInstructions(element, context);
            if (node.isArrayNode() && node.getAsArrayNode().get(0).isArrayNode()) {
                result.addAll(node.getAsArrayNode());
            } else {
                result.add(node);
            }
        }

        return result;
    }

    private JsonNode scanInstructions(JsonNode element, ImportResolverContext context) throws HistoneException {
        if (isString(element)) {
            return element;
        }

        if (!element.isArrayNode()) {
            Histone.runtime_log_warn("Invalid JSON element! Neither 'string', nor 'array'. Element: '{}'", element.toString());
            return element;
        }

        ArrayNode astArray = element.getAsArrayNode();

        int nodeType = getNodeType(astArray);
        switch (nodeType) {

            case AstNodeType.IMPORT:
                return resolveImport(astArray.get(1), context);

            default:
                return astArray;
        }
    }


    private ArrayNode resolveImport(JsonNode pathElement, ImportResolverContext context) throws HistoneException {
        if (!isString(pathElement)) {
            Histone.runtime_log_warn("Invalid path to imported template: '{}'", pathElement.toString());
            return AstNodeFactory.createNode(AstNodeType.STRING, "");
        }
        String path = pathElement.getAsJsonPrimitive().getAsString();
        Resource resource = null;
        InputStream resourceStream = null;
        try {
            String currentBaseURI = getContextBaseURI(context);
            String resourceFullPath = resourceLoader.resolveFullPath(path, currentBaseURI);

            if (context.hasImportedResource(resourceFullPath.toString())) {
                Histone.runtime_log_info("Resource already imported.");
                return AstNodeFactory.createNode(AstNodeType.STRING, "");
            } else {
                if (currentBaseURI == null) {
//                    if (!resourceLoader.isCacheable(path, null)) {
//                        String fullPath = resourceLoader.resolveFullPath(path, null);
//                        return AstNodeFactory.createNode(AstNodeType.IMPORT, fullPath);
//                    }
                } else {
//                    if (!resourceLoader.isCacheable(currentBaseURI, path)) {
//                        String fullPath = resourceLoader.resolveFullPath(path, currentBaseURI);
//                        return AstNodeFactory.createNode(AstNodeType.IMPORT, fullPath);
//                    }
                }
                resource = resourceLoader.load(path, currentBaseURI);

                if (resource == null) {
                    Histone.runtime_log_warn("Can't import resource by path = '{}'. Resource was not found.", path);
                    return AstNodeFactory.createNode(AstNodeType.STRING, "");
                }
                resourceStream = resource.getInputStream();
                if (resourceStream == null) {
                    Histone.runtime_log_warn("Can't import resource by path = '{}'. Resource is unreadable", path);
                    return AstNodeFactory.createNode(AstNodeType.STRING, "");
                }
                String templateContent = IOUtils.toString(resourceStream); //yeah... full file reading, because of our tokenizer is regexp-based :(

                // Add this resource full path to context
                context.addImportedResource(resourceFullPath.toString());

                ArrayNode parseResult = parser.parse(templateContent).getAsArrayNode();
                URI resourceURI = null; //TODO: refactor //resource.getURI();
                if (resourceURI != null && resourceURI.isAbsolute() && !resourceURI.isOpaque()) {
                    context.setBaseURI(resourceURI.resolve("").toString());
                }


                ArrayNode result = new ArrayNode();
                for (JsonNode elem : parseResult) {
                    if (elem.isArrayNode()) {
                        int nodeType = getNodeType(elem.getAsArrayNode());
                        switch (nodeType) {
                            case AstNodeType.MACRO:
                                result.add(elem);
                                break;
                            case AstNodeType.IMPORT:
                                ArrayNode resolvedAst = resolveImport(elem.getAsArrayNode().get(1), context);
                                if (resolvedAst.get(0).isArrayNode()) {
                                    result.addAll(resolvedAst);
                                } else {
                                    result.add(resolvedAst);
                                }
                                break;
                            default:
                                //do nothing
                        }

                    }
                }

                context.setBaseURI(currentBaseURI == null ? null : currentBaseURI.toString());

                return result;
            }
        } catch (ResourceLoadException e) {
            Histone.runtime_log_warn_e("Resource import failed! Unresolvable resource.", e);
            return AstNodeFactory.createNode(AstNodeType.STRING, "");
        } catch (IOException e) {
            Histone.runtime_log_warn_e("Resource import failed! Resource reading error.", e);
            return AstNodeFactory.createNode(AstNodeType.STRING, "");
        } catch (ParserException e) {
            Histone.runtime_log_warn_e("Resource import failed! Resource parsing error.", e);
            return AstNodeFactory.createNode(AstNodeType.STRING, "");
        } finally {
            IOUtils.closeQuietly(resourceStream, log);
            IOUtils.closeQuietly(resource, log);
        }
    }

    private boolean isString(JsonNode element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private int getNodeType(ArrayNode astArray) {
        return astArray.get(0).getAsJsonPrimitive().getAsInt();
    }

    private String getContextBaseURI(ImportResolverContext context) {
        String value = context.getBaseURI();
        if (value == null) {
            return null;
        }
        return value;
    }
      */
}
