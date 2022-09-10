/*
 * The MIT License
 *
 * Copyright (c) 2009-2022 PrimeTek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.primefaces.component.breadcrumb;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.primefaces.component.api.UIOutcomeTarget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.component.menu.BaseMenuRenderer;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.seo.JsonLDItem;
import org.primefaces.model.seo.JsonLDModel;
import org.primefaces.seo.JsonLD;
import org.primefaces.util.ComponentUtils;
import org.primefaces.util.HTML;
import org.primefaces.util.WidgetBuilder;

public class BreadCrumbRenderer extends BaseMenuRenderer {

    @Override
    protected void encodeMarkup(FacesContext context, AbstractMenu menu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        BreadCrumb breadCrumb = (BreadCrumb) menu;
        String clientId = breadCrumb.getClientId(context);
        String styleClass = getStyleClassBuilder(context)
                    .add(BreadCrumb.CONTAINER_CLASS)
                    .add(breadCrumb.getStyleClass())
                    .build();
        int elementCount = menu.getElementsCount();
        List<MenuElement> menuElements = menu.getElements();
        boolean isIconHome = breadCrumb.getHomeDisplay().equals("icon");
        String wrapper = "nav";
        String listType = "ol";

        // SEO
        boolean isSEO = breadCrumb.isSeo();
        List<JsonLDItem> ldItems = new ArrayList<>();

        //home icon for first item
        if (isIconHome && elementCount > 0) {
            String icon = breadCrumb.getHomeIcon();
            String iconStyleClass = "ui-breadcrumb-home-icon " + icon;
            ((MenuItem) menuElements.get(0)).setStyleClass(iconStyleClass);
        }

        writer.startElement(wrapper, null);
        writer.writeAttribute("id", clientId, null);
        writer.writeAttribute("class", styleClass, null);
        writer.writeAttribute(HTML.ARIA_LABEL, HTML.ARIA_LABEL_BREADCRUMB, null);
        if (breadCrumb.getStyle() != null) {
            writer.writeAttribute("style", breadCrumb.getStyle(), null);
        }

        if (elementCount > 0) {
            writer.startElement(listType, null);
            writer.writeAttribute("class", BreadCrumb.ITEMS_CLASS, null);

            for (int i = 0; i < elementCount; i++) {
                MenuElement element = menuElements.get(i);

                if (element.isRendered() && element instanceof MenuItem) {
                    MenuItem item = (MenuItem) element;

                    if (isSEO) {
                        ldItems.add(new JsonLDItem("ListItem", ldItems.size() + 1, item.getValue(), getTargetRequestURL(context, (UIOutcomeTarget) item)));
                    }

                    writer.startElement("li", null);

                    boolean last = i + 1 == elementCount;
                    if (item.isDisabled() || (breadCrumb.isLastItemDisabled() && last)) {
                        encodeDisabledMenuItem(context, item);
                    }
                    else {
                        Entry<String, String> attr = null;
                        if (last) {
                            attr = new SimpleEntry<>(HTML.ARIA_CURRENT, HTML.ARIA_CURRENT_PAGE);
                        }
                        encodeMenuItem(context, menu, item, menu.getTabindex(), attr);
                    }

                    writer.endElement("li");
                }
            }

            UIComponent optionsFacet = menu.getFacet("options");
            if (ComponentUtils.shouldRenderFacet(optionsFacet)) {
                writer.startElement("li", null);
                writer.writeAttribute("class", BreadCrumb.OPTIONS_CLASS, null);
                optionsFacet.encodeAll(context);
                writer.endElement("li");
            }

            writer.endElement(listType);
        }

        writer.endElement(wrapper);

        if (isSEO) {
            JsonLDModel ldModel = new JsonLDModel("https://schema.org", "BreadcrumbList", "itemListElement", ldItems);
            JsonLD.encode(context, ldModel, clientId + "_seo");
        }
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        // Do nothing
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    protected void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException {
        BreadCrumb breadcrumb = (BreadCrumb) abstractMenu;

        WidgetBuilder wb = getWidgetBuilder(context);
        wb.init("BreadCrumb", breadcrumb)
                .attr("seo", breadcrumb.isSeo());

        wb.finish();
    }

    private void encodeDisabledMenuItem(FacesContext context, MenuItem menuItem) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        String style = menuItem.getStyle();
        String styleClass = getStyleClassBuilder(context)
                    .add(BreadCrumb.MENUITEM_LINK_CLASS)
                    .add(menuItem.getStyleClass())
                    .add("ui-state-disabled")
                    .build();

        writer.startElement("span", null); // outer span
        writer.writeAttribute("class", styleClass, null);
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }

        String icon = menuItem.getIcon();
        Object value = menuItem.getValue();

        if (icon != null) {
            writer.startElement("span", null);
            writer.writeAttribute("class", BreadCrumb.MENUITEM_ICON_CLASS + " " + icon, null);
            writer.writeAttribute(HTML.ARIA_HIDDEN, "true", null);
            writer.endElement("span");
        }

        writer.startElement("span", null);
        writer.writeAttribute("class", BreadCrumb.MENUITEM_TEXT_CLASS, null);

        if (value != null) {
            if (menuItem.isEscape()) {
                writer.writeText(value, "value");
            }
            else {
                writer.write(value.toString());
            }
        }
        writer.endElement("span"); // text span
        writer.endElement("span"); // outer span
    }
}
