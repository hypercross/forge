package forge.gui.toolbox.itemmanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JTable;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import forge.gui.toolbox.itemmanager.table.TableColumnInfo;
import forge.gui.toolbox.itemmanager.table.SColumnUtil;
import forge.gui.toolbox.itemmanager.table.SColumnUtil.ColumnName;
import forge.gui.toolbox.itemmanager.table.SColumnUtil.SortState;
import forge.item.InventoryItem;
import forge.properties.NewConstants;

/** 
 * Handles editor preferences saving and loading.
 * 
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SItemManagerIO {
    /** Used in the XML IO to extract properties from PREFS file. */
    private enum ColumnProperty { /** */
        enumval, /** */
        identifier, /** */
        show, /** */
        index, /** */
        sortpriority, /** */
        sortstate, /** */
        width
    }

    /** Preferences (must match with PREFS file). */
    public enum EditorPreference {
        stats_deck,
        display_unique_only,
        elastic_columns
    }

    private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLEvent NEWLINE = EVENT_FACTORY.createDTD("\n");
    private static final XMLEvent TAB = EVENT_FACTORY.createDTD("\t");

    private static final Map<EditorPreference, Boolean> PREFS
        = new HashMap<EditorPreference, Boolean>();

    private static final Map<ColumnName, TableColumnInfo<InventoryItem>> COLS
        = new TreeMap<ColumnName, TableColumnInfo<InventoryItem>>();

    /**
     * Retrieve a preference from the editor preference map.
     * 
     * @param name0 &emsp; {@link forge.gui.toolbox.itemmanager.SItemManagerUtil.EditorPreference}
     * @return TableColumnInfo<InventoryItem>
     */
    public static boolean getPref(final EditorPreference name0) {
        return PREFS.get(name0);
    }

    /**
     * Set a preference in the editor preference map.
     * 
     * @param name0 &emsp; {@link forge.gui.toolbox.itemmanager.SItemManagerUtil.EditorPreference}
     * @param val0 &emsp; boolean
     */
    public static void setPref(final EditorPreference name0, final boolean val0) {
        PREFS.put(name0, val0);
    }

    /**
     * Retrieve a custom column.
     * 
     * @param name0 &emsp; {@link forge.gui.toolbox.itemmanager.SItemManagerUtil.CatalogColumnName}
     * @return TableColumnInfo<InventoryItem>
     */
    public static TableColumnInfo<InventoryItem> getColumn(final ColumnName name0) {
        return COLS.get(name0);
    }

    /** Publicly-accessible save method, to neatly handle exception handling. */
    public static void savePreferences(JTable table) {
        try { save(table); }
        catch (final Exception e) { e.printStackTrace(); }
    }

    /** Publicly-accessible load method, to neatly handle exception handling. */
    public static void loadPreferences() {
        try { load(); }
        catch (final Exception e) { e.printStackTrace(); }
    }

    /**
     * 
     * Save list view
     * 
     * @param table
     */
    private static void save(JTable table) throws Exception {
        final XMLOutputFactory out = XMLOutputFactory.newInstance();
        final XMLEventWriter writer = out.createXMLEventWriter(new FileOutputStream(NewConstants.EDITOR_PREFERENCES_FILE.userPrefLoc));

        writer.add(EVENT_FACTORY.createStartDocument());
        writer.add(NEWLINE);
        writer.add(EVENT_FACTORY.createStartElement("", "", "preferences"));
        writer.add(EVENT_FACTORY.createAttribute("type", "editor"));
        writer.add(NEWLINE);

        for (final EditorPreference p : PREFS.keySet()) {
            writer.add(TAB);
            writer.add(EVENT_FACTORY.createStartElement("", "", "pref"));
            writer.add(EVENT_FACTORY.createAttribute(
                    "name", p.toString()));
            writer.add(EVENT_FACTORY.createAttribute(
                    "value", PREFS.get(p).toString()));
            writer.add(EVENT_FACTORY.createEndElement("", "", "pref"));
            writer.add(NEWLINE);
        }

        for (final ColumnName c : COLS.keySet()) {
            // If column is not in view, retain previous model index for the next time
            // that the column will be in the view.
            int index = SColumnUtil.getColumnViewIndex(table, c);
            if (index == -1) {
                index = COLS.get(c).getModelIndex();
            }
            COLS.get(c).setIndex(index); //update index property of column

            writer.add(TAB);
            writer.add(EVENT_FACTORY.createStartElement("", "", "col"));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.enumval.toString(), COLS.get(c).getEnumValue()));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.identifier.toString(), COLS.get(c).getIdentifier().toString()));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.show.toString(), String.valueOf(COLS.get(c).isShowing())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.index.toString(), String.valueOf(index)));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.sortpriority.toString(), String.valueOf(COLS.get(c).getSortPriority())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.sortstate.toString(), String.valueOf(COLS.get(c).getSortState())));
            writer.add(EVENT_FACTORY.createAttribute(
                    ColumnProperty.width.toString(), String.valueOf(COLS.get(c).getWidth())));
            writer.add(EVENT_FACTORY.createEndElement("", "", "col"));
            writer.add(NEWLINE);
        }

        writer.add(EVENT_FACTORY.createEndDocument());
        writer.flush();
        writer.close();
    }

    private static void load() throws Exception {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        PREFS.clear();
        COLS.clear();

        // read in defaults
        loadPrefs(inputFactory.createXMLEventReader(new FileInputStream(NewConstants.EDITOR_PREFERENCES_FILE.defaultLoc)));
        
        try {
            // overwrite defaults with user preferences, if they exist
            loadPrefs(inputFactory.createXMLEventReader(new FileInputStream(NewConstants.EDITOR_PREFERENCES_FILE.userPrefLoc)));
        } catch (FileNotFoundException e) {
            /* ignore; it's ok if this file doesn't exist */
        } finally {
            SColumnUtil.attachSortAndDisplayFunctions();
        }
    }
    
    private static void loadPrefs(final XMLEventReader reader) throws XMLStreamException {
        XMLEvent event;
        StartElement element;
        Iterator<?> attributes;
        Attribute attribute;
        EditorPreference pref;
        TableColumnInfo<InventoryItem> tempcol;
        String tagname;

        while (reader.hasNext()) {
            event = reader.nextEvent();

            if (event.isStartElement()) {
                element = event.asStartElement();
                tagname = element.getName().getLocalPart();

                // Assemble preferences
                if (tagname.equals("pref")) {
                    // Retrieve name of pref
                    attributes = element.getAttributes();
                    try {
                        pref = EditorPreference.valueOf(((Attribute) attributes.next()).getValue());

                        // Add to map
                        PREFS.put(pref, Boolean.valueOf(((Attribute) attributes.next()).getValue()));
                    } catch (IllegalArgumentException e) { /* ignore; just don't use */ }
                }
                // Assemble columns
                else if (tagname.equals("col")) {
                    attributes = element.getAttributes();
                    tempcol = new TableColumnInfo<InventoryItem>();

                    while (attributes.hasNext()) {
                        attribute = (Attribute) attributes.next();
                        if (attribute.getName().toString().equals(ColumnProperty.enumval.toString())) {
                            try { COLS.put(ColumnName.valueOf(attribute.getValue()), tempcol); }
                            catch (final Exception e) { /* ignore invalid entries */ }

                            tempcol.setEnumValue(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.identifier.toString())) {
                            tempcol.setIdentifier(attribute.getValue());
                            tempcol.setHeaderValue(attribute.getValue());
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.width.toString())) {
                            tempcol.setPreferredWidth(Integer.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.show.toString())) {
                            tempcol.setShowing(Boolean.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.index.toString())) {
                            tempcol.setIndex(Integer.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.sortpriority.toString())) {
                            tempcol.setSortPriority(Integer.valueOf(attribute.getValue()));
                        }
                        else if (attribute.getName().toString().equals(ColumnProperty.sortstate.toString())) {
                            tempcol.setSortState(SortState.valueOf(attribute.getValue().toString()));
                        }
                    }
                }
            }
        }
    }
}
