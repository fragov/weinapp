package com.wein3.weinapp.database;

import com.mapbox.services.commons.geojson.GeometryCollection;

import com.wein3.weinapp.Area;

import java.util.HashMap;

public class DummyDB implements Database {

    @Override
    public void init() {

    }

    @Override
    public HashMap<String, String> getListOfAreas() {
        HashMap<String, String> polygonsList = new HashMap<>();
        polygonsList.put("6516c43863164550b8ef9fdc053bfcbc", "Feldblock Gamma");
        polygonsList.put("74b215568b674b4d9cf50610d858d807", "Feldblock Beta");
        polygonsList.put("b041bfd8c51b40aaa3bab31a5aa99c79", "Feldblock Omega");
        polygonsList.put("cebcb109b77f4e07917a140ece2a10e6", "Feldblock Alpha");
        return polygonsList;
    }

    @Override
    public Area getAreaById(String id) {
        Area area = new Area();
        switch (id) {
            case "6516c43863164550b8ef9fdc053bfcbc":
                area.setId("6516c43863164550b8ef9fdc053bfcbc");
                area.setRev("5-c3ff0918b103351dbbe54b8098e3cd69");
                area.setDescription("Feldblock Gamma");
                area.setGewann("Burg Klopp");
                area.setSize(68175.32);
                String json = "{\"type\":\"FeatureCollection\",\"features\":[{\"id\":\"b22f4ded896ebb0b213edefb39c7f666\",\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"coordinates\":[[[7.898658135370539,49.96623314659044],[7.898254097020498,49.9659745838681],[7.898275736283807,49.965125961905585],[7.897626400203876,49.964914466437904],[7.896842172257976,49.96306474500574],[7.896879516453339,49.96241612455549],[7.897906481619316,49.963184858945766],[7.900352525938359,49.964073692782705],[7.902112572104784,49.96418835377648],[7.9022500496380985,49.96441288184809],[7.901329427832479,49.965164065644814],[7.900897257007642,49.96526563729009],[7.898658135370539,49.96623314659044]]],\"type\":\"Polygon\"}}]}";
                area.setGeometry(GeometryCollection.fromJson(json));
                area.setGrowingRegion("Rheinhessen");
                area.setCurrentUsage("Weinberg");
                area.setCurrentLandUsage("Direktzug");
                area.setCategory("Ertragsfläche");
                area.setChannelWidth(2);
                area.setWineRowsCount(30);
                area.setWineRowsLength(60);
                area.setVinesCount(75);
                area.setUsefullLife(100);
                area.setBusiness("local_business");
        }
        return area;
    }

    @Override
    public String insertArea(Area area) {
        return "cebcb109b77f4e07917a140ece2a10e6";
    }

    @Override
    public void updateArea(Area area) {

    }

    @Override
    public void removeAreaById(String id) {

    }

    @Override
    public void close() {

    }

}
