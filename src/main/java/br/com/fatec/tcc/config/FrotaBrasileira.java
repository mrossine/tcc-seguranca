package br.com.fatec.tcc.config;

import java.util.*;

/**
 * Catálogo estático das 20 marcas com maior frota circulante no Brasil
 * e seus 15 modelos mais presentes em cada uma (300 combinações).
 */
public final class FrotaBrasileira {

    private FrotaBrasileira() {}

    public static final Map<String, List<String>> CATALOGO;

    static {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("Fiat", List.of(
            "Palio","Uno","Strada","Siena","Fiorino",
            "Toro","Argo","Mobi","Idea","Punto",
            "Cronos","Doblò","Bravo","500","Linea"
        ));
        m.put("Volkswagen", List.of(
            "Gol","Fox","Voyage","Saveiro","Polo",
            "Kombi","Fusca","up!","T-Cross","Virtus",
            "Jetta","Nivus","Amarok","Tiguan","Parati"
        ));
        m.put("Chevrolet", List.of(
            "Celta","Corsa","Classic","Onix","Prisma",
            "S10","Montana","Spin","Agile","Cobalt",
            "Cruze","Tracker","Vectra","Astra","Zafira"
        ));
        m.put("Ford", List.of(
            "Ka","Fiesta","EcoSport","Focus","Ranger",
            "Fusion","Escort","Del Rey","Corcel","F-1000",
            "Edge","Territory","Bronco Sport","Mustang","Mondeo"
        ));
        m.put("Toyota", List.of(
            "Corolla","Hilux","Etios","SW4","Yaris",
            "Corolla Cross","RAV4","Camry","Prius","Land Cruiser",
            "Bandeirante","FJ Cruiser","Hilux SW4","Highlander","C-HR"
        ));
        m.put("Honda", List.of(
            "Civic","Fit","City","HR-V","CR-V",
            "Accord","WR-V","Prelude","CR-Z","Civic Type R",
            "Insight","S2000","Odyssey","Element","NSX"
        ));
        m.put("Renault", List.of(
            "Clio","Sandero","Logan","Duster","Kwid",
            "Oroch","Captur","Fluence","Mégane","Scénic",
            "Symbol","Kangoo","Master","Laguna","Twingo"
        ));
        m.put("Hyundai", List.of(
            "HB20","Creta","Tucson","Santa Fe","i30",
            "Elantra","Veloster","Azera","Kona","HR",
            "Veracruz","i40","Palisade","Ioniq","Genesis"
        ));
        m.put("Jeep", List.of(
            "Renegade","Compass","Wrangler","Commander","Cherokee",
            "Grand Cherokee","Patriot","Liberty","Gladiator","Willys",
            "CJ-5","Wagoneer","Grand Wagoneer","Comanche","Jeepster"
        ));
        m.put("Nissan", List.of(
            "March","Versa","Kicks","Frontier","Sentra",
            "Tiida","Livina","Grand Livina","X-Terra","Murano",
            "Pathfinder","350Z/370Z","GT-R","Leaf","NV200"
        ));
        m.put("Citroën", List.of(
            "C3","C4 Cactus","C3 Aircross","C4 Lounge","C3 Picasso",
            "Xsara Picasso","C4 Pallas","Xsara","ZX","C5",
            "C6","DS3","DS4","AX","C4 VTR"
        ));
        m.put("Peugeot", List.of(
            "206","207","208","307","308",
            "408","2008","3008","306","406",
            "407","508","5008","Partner","Expert"
        ));
        m.put("Mitsubishi", List.of(
            "L200 Triton","Pajero Sport","Pajero Full","Pajero Dakar","ASX",
            "Lancer","Outlander","Eclipse Cross","Galant","Space Wagon",
            "L300","Eclipse","Airtrek","Mirage","Pajero TR4"
        ));
        m.put("Kia", List.of(
            "Sportage","Cerato","Picanto","Soul","Sorento",
            "Rio","Carens","Mohave","Carnival","Cadenza",
            "Optima","K2500","Stonic","Niro","Stinger"
        ));
        m.put("BMW", List.of(
            "Série 3","X1","X3","Série 1","X5",
            "Série 2","X2","X4","X6","Z4",
            "i3","Série 5","Série 7","M3","iX3"
        ));
        m.put("Mercedes-Benz", List.of(
            "Classe C","Classe A","GLA","GLC","Sprinter",
            "GLB","Classe E","CLA","Vito","GLE",
            "GLS","Classe S","SLK","Viano","AMG GT"
        ));
        m.put("Audi", List.of(
            "A3","A4","Q3","Q5","A1",
            "Q7","A5","A6","Q8","TT",
            "e-tron","Q2","R8","S3","RS3"
        ));
        m.put("Land Rover", List.of(
            "Discovery Sport","Range Rover Evoque","Range Rover Velar","Range Rover Sport","Range Rover",
            "Discovery","Defender","Freelander 2","Freelander","Discovery 3",
            "Discovery 4","Range Rover Classic","Range Rover P38","Série I/II/III","LR2"
        ));
        m.put("Chery", List.of(
            "QQ","Tiggo 2","Tiggo 5X","Tiggo 7","Tiggo 8",
            "Celer","Arrizo 5","Arrizo 6","Tiggo 3X","iCar",
            "Face","S18","V5","Bonus","eQ"
        ));
        m.put("Volvo", List.of(
            "XC60","XC40","XC90","S60","V40",
            "C30","S40","V60","S90","V90",
            "C40","XC70","240","850","P1800"
        ));

        CATALOGO = Collections.unmodifiableMap(m);
    }

    public static List<String> getMarcas() {
        return new ArrayList<>(CATALOGO.keySet());
    }

    public static List<String> getModelos(String marca) {
        return CATALOGO.getOrDefault(marca, List.of());
    }

    public static boolean marcaExiste(String marca) {
        return CATALOGO.containsKey(marca);
    }

    public static boolean modeloExiste(String marca, String modelo) {
        return CATALOGO.getOrDefault(marca, List.of()).contains(modelo);
    }
}
