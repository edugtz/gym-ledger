package com.edu.gymledger.data.reference

import com.edu.gymledger.domain.model.FoodReference

object FoodReferenceSeed {

    val foods: List<FoodReference> = listOf(
        FoodReference(
            id = "whole_egg_large",
            name = "Whole egg, large",
            aliases = listOf("egg", "huevo", "huevo entero"),
            caloriesPer100g = 155,
            proteinPer100g = 13.0,
            carbsPer100g = 1.1,
            fatPer100g = 11.0,
            gramsPerUnit = 50.0,
            unitLabel = "large egg"
        ),
        FoodReference(
            id = "egg_white",
            name = "Egg white",
            aliases = listOf("egg whites", "clara de huevo"),
            caloriesPer100g = 52,
            proteinPer100g = 11.0,
            carbsPer100g = 0.7,
            fatPer100g = 0.2,
            gramsPerUnit = 33.0,
            unitLabel = "egg white"
        ),
        FoodReference(
            id = "chicken_breast_cooked",
            name = "Chicken breast, cooked",
            aliases = listOf("chicken", "pechuga de pollo", "pollo"),
            caloriesPer100g = 165,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6
        ),
        FoodReference(
            id = "chicken_breast_raw",
            name = "Chicken breast, raw",
            aliases = listOf("raw chicken", "pechuga cruda"),
            caloriesPer100g = 120,
            proteinPer100g = 22.5,
            carbsPer100g = 0.0,
            fatPer100g = 2.6
        ),
        FoodReference(
            id = "ground_beef_lean",
            name = "Ground beef, lean",
            aliases = listOf("lean beef", "carne molida magra", "res magra"),
            caloriesPer100g = 250,
            proteinPer100g = 26.0,
            carbsPer100g = 0.0,
            fatPer100g = 15.0
        ),
        FoodReference(
            id = "salmon_cooked",
            name = "Salmon, cooked",
            aliases = listOf("salmon", "salmón"),
            caloriesPer100g = 208,
            proteinPer100g = 20.0,
            carbsPer100g = 0.0,
            fatPer100g = 13.0
        ),
        FoodReference(
            id = "tuna_canned",
            name = "Tuna, canned in water",
            aliases = listOf("canned tuna", "atún en lata"),
            caloriesPer100g = 116,
            proteinPer100g = 26.0,
            carbsPer100g = 0.0,
            fatPer100g = 1.0
        ),
        FoodReference(
            id = "greek_yogurt_plain",
            name = "Greek yogurt, plain",
            aliases = listOf("greek yogurt", "yogur griego"),
            caloriesPer100g = 97,
            proteinPer100g = 9.0,
            carbsPer100g = 3.6,
            fatPer100g = 5.0,
            gramsPerUnit = 170.0,
            unitLabel = "container"
        ),
        FoodReference(
            id = "milk_whole",
            name = "Whole milk",
            aliases = listOf("milk", "leche entera", "leche"),
            caloriesPer100g = 61,
            proteinPer100g = 3.2,
            carbsPer100g = 4.8,
            fatPer100g = 3.3,
            gramsPerUnit = 244.0,
            unitLabel = "cup"
        ),
        FoodReference(
            id = "cheddar_cheese",
            name = "Cheddar cheese",
            aliases = listOf("cheese", "queso cheddar", "queso"),
            caloriesPer100g = 403,
            proteinPer100g = 25.0,
            carbsPer100g = 1.3,
            fatPer100g = 33.0,
            gramsPerUnit = 28.0,
            unitLabel = "slice"
        ),
        FoodReference(
            id = "white_rice_cooked",
            name = "White rice, cooked",
            aliases = listOf("rice", "arroz blanco", "arroz"),
            caloriesPer100g = 130,
            proteinPer100g = 2.7,
            carbsPer100g = 28.0,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "brown_rice_cooked",
            name = "Brown rice, cooked",
            aliases = listOf("brown rice", "arroz integral"),
            caloriesPer100g = 123,
            proteinPer100g = 2.7,
            carbsPer100g = 26.0,
            fatPer100g = 1.0
        ),
        FoodReference(
            id = "oats_dry",
            name = "Oats, dry",
            aliases = listOf("oatmeal", "avena"),
            caloriesPer100g = 389,
            proteinPer100g = 16.9,
            carbsPer100g = 66.3,
            fatPer100g = 6.9,
            gramsPerUnit = 40.0,
            unitLabel = "serving"
        ),
        FoodReference(
            id = "whole_wheat_bread",
            name = "Whole wheat bread",
            aliases = listOf("wheat bread", "pan integral", "pan"),
            caloriesPer100g = 247,
            proteinPer100g = 13.0,
            carbsPer100g = 41.0,
            fatPer100g = 3.4,
            gramsPerUnit = 36.0,
            unitLabel = "slice"
        ),
        FoodReference(
            id = "white_bread",
            name = "White bread",
            aliases = listOf("pan blanco"),
            caloriesPer100g = 265,
            proteinPer100g = 9.0,
            carbsPer100g = 49.0,
            fatPer100g = 3.2,
            gramsPerUnit = 30.0,
            unitLabel = "slice"
        ),
        FoodReference(
            id = "pasta_cooked",
            name = "Pasta, cooked",
            aliases = listOf("spaghetti", "pasta", "fideos"),
            caloriesPer100g = 131,
            proteinPer100g = 5.0,
            carbsPer100g = 25.0,
            fatPer100g = 1.1
        ),
        FoodReference(
            id = "sweet_potato_cooked",
            name = "Sweet potato, cooked",
            aliases = listOf("sweet potato", "batata", "camote"),
            caloriesPer100g = 90,
            proteinPer100g = 2.0,
            carbsPer100g = 21.0,
            fatPer100g = 0.1
        ),
        FoodReference(
            id = "potato_boiled",
            name = "Potato, boiled",
            aliases = listOf("potato", "papa", "patata"),
            caloriesPer100g = 87,
            proteinPer100g = 1.9,
            carbsPer100g = 20.0,
            fatPer100g = 0.1
        ),
        FoodReference(
            id = "banana",
            name = "Banana",
            aliases = listOf("plátano", "banano"),
            caloriesPer100g = 89,
            proteinPer100g = 1.1,
            carbsPer100g = 23.0,
            fatPer100g = 0.3,
            gramsPerUnit = 118.0,
            unitLabel = "medium banana"
        ),
        FoodReference(
            id = "apple",
            name = "Apple",
            aliases = listOf("manzana"),
            caloriesPer100g = 52,
            proteinPer100g = 0.3,
            carbsPer100g = 14.0,
            fatPer100g = 0.2,
            gramsPerUnit = 182.0,
            unitLabel = "medium apple"
        ),
        FoodReference(
            id = "orange",
            name = "Orange",
            aliases = listOf("naranja"),
            caloriesPer100g = 47,
            proteinPer100g = 0.9,
            carbsPer100g = 12.0,
            fatPer100g = 0.1,
            gramsPerUnit = 131.0,
            unitLabel = "medium orange"
        ),
        FoodReference(
            id = "blueberries",
            name = "Blueberries",
            aliases = listOf("blueberry", "arándanos"),
            caloriesPer100g = 57,
            proteinPer100g = 0.7,
            carbsPer100g = 14.5,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "strawberries",
            name = "Strawberries",
            aliases = listOf("strawberry", "fresas"),
            caloriesPer100g = 32,
            proteinPer100g = 0.7,
            carbsPer100g = 7.7,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "avocado",
            name = "Avocado",
            aliases = listOf("aguacate", "palta"),
            caloriesPer100g = 160,
            proteinPer100g = 2.0,
            carbsPer100g = 8.5,
            fatPer100g = 14.7,
            gramsPerUnit = 150.0,
            unitLabel = "half avocado"
        ),
        FoodReference(
            id = "almonds",
            name = "Almonds",
            aliases = listOf("almond", "almendras"),
            caloriesPer100g = 579,
            proteinPer100g = 21.2,
            carbsPer100g = 21.6,
            fatPer100g = 49.9,
            gramsPerUnit = 28.0,
            unitLabel = "oz"
        ),
        FoodReference(
            id = "peanut_butter",
            name = "Peanut butter",
            aliases = listOf("mantequilla de maní", "mantequilla de cacahuate"),
            caloriesPer100g = 588,
            proteinPer100g = 25.0,
            carbsPer100g = 20.0,
            fatPer100g = 50.0,
            gramsPerUnit = 32.0,
            unitLabel = "tbsp"
        ),
        FoodReference(
            id = "olive_oil",
            name = "Olive oil",
            aliases = listOf("oil", "aceite de oliva", "aceite"),
            caloriesPer100g = 884,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 100.0,
            gramsPerUnit = 14.0,
            unitLabel = "tbsp"
        ),
        FoodReference(
            id = "butter",
            name = "Butter",
            aliases = listOf("mantequilla"),
            caloriesPer100g = 717,
            proteinPer100g = 0.9,
            carbsPer100g = 0.1,
            fatPer100g = 81.0,
            gramsPerUnit = 14.0,
            unitLabel = "tbsp"
        ),
        FoodReference(
            id = "whey_protein_powder",
            name = "Whey protein powder",
            aliases = listOf("protein powder", "proteína en polvo", "whey"),
            caloriesPer100g = 400,
            proteinPer100g = 80.0,
            carbsPer100g = 10.0,
            fatPer100g = 6.0,
            gramsPerUnit = 30.0,
            unitLabel = "scoop"
        ),
        FoodReference(
            id = "cottage_cheese",
            name = "Cottage cheese",
            aliases = listOf("queso cottage", "requesón"),
            caloriesPer100g = 98,
            proteinPer100g = 11.0,
            carbsPer100g = 3.4,
            fatPer100g = 4.3
        ),
        FoodReference(
            id = "tofu",
            name = "Tofu, firm",
            aliases = listOf("soy curd", "tofu firme"),
            caloriesPer100g = 76,
            proteinPer100g = 8.0,
            carbsPer100g = 1.9,
            fatPer100g = 4.2
        ),
        FoodReference(
            id = "black_beans_cooked",
            name = "Black beans, cooked",
            aliases = listOf("black beans", "frijoles negros"),
            caloriesPer100g = 132,
            proteinPer100g = 8.9,
            carbsPer100g = 23.7,
            fatPer100g = 0.5
        ),
        FoodReference(
            id = "chickpeas_cooked",
            name = "Chickpeas, cooked",
            aliases = listOf("chickpeas", "garbanzos"),
            caloriesPer100g = 164,
            proteinPer100g = 8.9,
            carbsPer100g = 27.4,
            fatPer100g = 2.6
        ),
        FoodReference(
            id = "lentils_cooked",
            name = "Lentils, cooked",
            aliases = listOf("lentils", "lentejas"),
            caloriesPer100g = 116,
            proteinPer100g = 9.0,
            carbsPer100g = 20.1,
            fatPer100g = 0.4
        ),
        FoodReference(
            id = "quinoa_cooked",
            name = "Quinoa, cooked",
            aliases = listOf("quinua", "kinwa"),
            caloriesPer100g = 120,
            proteinPer100g = 4.4,
            carbsPer100g = 21.3,
            fatPer100g = 1.9
        ),
        FoodReference(
            id = "broccoli",
            name = "Broccoli, raw",
            aliases = listOf("brócoli"),
            caloriesPer100g = 34,
            proteinPer100g = 2.8,
            carbsPer100g = 7.0,
            fatPer100g = 0.4
        ),
        FoodReference(
            id = "spinach_raw",
            name = "Spinach, raw",
            aliases = listOf("spinach", "espinacas"),
            caloriesPer100g = 23,
            proteinPer100g = 2.9,
            carbsPer100g = 3.6,
            fatPer100g = 0.4
        ),
        FoodReference(
            id = "tomato",
            name = "Tomato",
            aliases = listOf("tomate", "jitomate"),
            caloriesPer100g = 18,
            proteinPer100g = 0.9,
            carbsPer100g = 3.9,
            fatPer100g = 0.2,
            gramsPerUnit = 123.0,
            unitLabel = "medium tomato"
        ),
        FoodReference(
            id = "carrot",
            name = "Carrot",
            aliases = listOf("carrots", "zanahoria", "zanahorias"),
            caloriesPer100g = 41,
            proteinPer100g = 0.9,
            carbsPer100g = 9.6,
            fatPer100g = 0.2,
            gramsPerUnit = 61.0,
            unitLabel = "medium carrot"
        ),
        FoodReference(
            id = "onion",
            name = "Onion",
            aliases = listOf("onions", "cebolla"),
            caloriesPer100g = 40,
            proteinPer100g = 1.1,
            carbsPer100g = 9.3,
            fatPer100g = 0.1
        ),
        FoodReference(
            id = "bell_pepper",
            name = "Bell pepper",
            aliases = listOf("peppers", "pimiento", "pimentón"),
            caloriesPer100g = 31,
            proteinPer100g = 1.0,
            carbsPer100g = 6.0,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "mushrooms",
            name = "Mushrooms, raw",
            aliases = listOf("mushroom", "champiñones", "hongos"),
            caloriesPer100g = 22,
            proteinPer100g = 3.1,
            carbsPer100g = 3.3,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "honey",
            name = "Honey",
            aliases = listOf("miel"),
            caloriesPer100g = 304,
            proteinPer100g = 0.3,
            carbsPer100g = 82.4,
            fatPer100g = 0.0,
            gramsPerUnit = 21.0,
            unitLabel = "tbsp"
        ),
        FoodReference(
            id = "sugar_white",
            name = "White sugar",
            aliases = listOf("sugar", "azúcar"),
            caloriesPer100g = 387,
            proteinPer100g = 0.0,
            carbsPer100g = 100.0,
            fatPer100g = 0.0,
            gramsPerUnit = 12.5,
            unitLabel = "tsp"
        ),
        FoodReference(
            id = "dark_chocolate",
            name = "Dark chocolate, 70-85%",
            aliases = listOf("dark chocolate", "chocolate oscuro", "chocolate negro"),
            caloriesPer100g = 598,
            proteinPer100g = 7.8,
            carbsPer100g = 45.9,
            fatPer100g = 42.6,
            gramsPerUnit = 28.0,
            unitLabel = "oz"
        ),
        FoodReference(
            id = "popcorn_air_popped",
            name = "Popcorn, air-popped",
            aliases = listOf("popcorn", "palomitas"),
            caloriesPer100g = 387,
            proteinPer100g = 12.9,
            carbsPer100g = 77.9,
            fatPer100g = 4.7
        ),
        FoodReference(
            id = "tortilla_flour",
            name = "Flour tortilla",
            aliases = listOf("tortilla", "tortilla de harina"),
            caloriesPer100g = 312,
            proteinPer100g = 8.2,
            carbsPer100g = 52.0,
            fatPer100g = 8.5,
            gramsPerUnit = 45.0,
            unitLabel = "tortilla"
        ),
        FoodReference(
            id = "bacon_cooked",
            name = "Bacon, cooked",
            aliases = listOf("bacon", "tocino", "panceta"),
            caloriesPer100g = 541,
            proteinPer100g = 37.0,
            carbsPer100g = 1.4,
            fatPer100g = 42.0,
            gramsPerUnit = 12.0,
            unitLabel = "slice"
        ),
        FoodReference(
            id = "pork_chop_cooked",
            name = "Pork chop, cooked",
            aliases = listOf("pork", "chuleta de cerdo", "cerdo"),
            caloriesPer100g = 231,
            proteinPer100g = 27.0,
            carbsPer100g = 0.0,
            fatPer100g = 13.0
        ),
        FoodReference(
            id = "shrimp_cooked",
            name = "Shrimp, cooked",
            aliases = listOf("shrimp", "camarones", "gambas"),
            caloriesPer100g = 99,
            proteinPer100g = 24.0,
            carbsPer100g = 0.2,
            fatPer100g = 0.3
        ),
        FoodReference(
            id = "tilapia_cooked",
            name = "Tilapia, cooked",
            aliases = listOf("tilapia", "pescado tilapia"),
            caloriesPer100g = 128,
            proteinPer100g = 26.0,
            carbsPer100g = 0.0,
            fatPer100g = 2.7
        )
    )
}
