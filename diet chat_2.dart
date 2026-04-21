def get_diet_chart():
    # A dictionary mapping days to meal plans
    diet_plan = {
        "Monday": {
            "Breakfast": "Oatmeal with berries and walnuts",
            "Lunch": "Grilled chicken salad with olive oil dressing",
            "Snack": "Apple slices with almond butter",
            "Dinner": "Baked salmon with quinoa and steamed broccoli"
        },
        "Tuesday": {
            "Breakfast": "Greek yogurt with honey and flax seeds",
            "Lunch": "Turkey and avocado wrap (whole grain)",
            "Snack": "A handful of raw almonds",
            "Dinner": "Lentil soup with a side of sautéed spinach"
        },
        "Wednesday": {
            "Breakfast": "Scrambled eggs with spinach and whole-toast",
            "Lunch": "Quinoa bowl with chickpeas, cucumber, and feta",
            "Snack": "Cottage cheese with pineapple",
            "Dinner": "Lean beef stir-fry with mixed vegetables"
        },
        # You can add the rest of the week here...
    }
    return diet_plan

def display_day(day, plan):
    day = day.capitalize()
    if day in plan:
        print(f"\n--- {day} Diet Plan ---")
        for meal, food in plan[day].items():
            print(f"**{meal}**: {food}")
    else:
        print("Day not found. Please enter a valid day of the week.")

# --- Execution ---
my_plan = get_diet_chart()
user_input = input("Enter a day to see your meal plan (e.g., Monday): ")
display_day(user_input, my_plan)
