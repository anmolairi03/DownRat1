package com.example.cycletracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WELLNESS_TIPS = listOf(
    "Stay hydrated! Drinking water helps reduce bloating during your period.",
    "Try a warm compress or heating pad on your lower abdomen to ease cramps.",
    "Gentle stretching or yoga can help alleviate lower back pain and cramping.",
    "Increase your iron intake with foods like spinach, lentils, or red meat.",
    "Ginger tea is known to help reduce inflammation and ease menstrual pain.",
    "Prioritize sleep! Aim for 7-9 hours to help your body recover.",
    "Dark chocolate (70% or more) can satisfy cravings and boost magnesium.",
    "Limit caffeine and alcohol, which can exacerbate bloating and mood swings.",
    "Take short walks to boost endorphins, your body's natural painkillers.",
    "A warm bath with Epsom salts can relax tense muscles and soothe cramps.",
    "Snack on bananas; they are rich in potassium which helps prevent cramping.",
    "Track your symptoms regularly to identify your unique patterns.",
    "Try chamomile tea before bed to improve sleep quality and reduce stress.",
    "Omega-3 fatty acids from fish or chia seeds can help reduce inflammation.",
    "Practice deep breathing exercises to help manage mood swings and stress.",
    "Wear comfortable, loose-fitting clothing when you're feeling bloated.",
    "Massaging your lower abdomen with essential oils like lavender can help.",
    "Eat smaller, more frequent meals to keep blood sugar levels stable.",
    "Avoid highly salty foods to prevent additional water retention and bloating.",
    "Consider a magnesium supplement after consulting with your doctor.",
    "Fennel seeds or fennel tea might help ease digestive issues during your cycle.",
    "Peppermint tea can soothe an upset stomach and relieve nausea.",
    "If cramps are severe, over-the-counter NSAIDs like ibuprofen can help.",
    "Try to stick to a consistent sleep schedule even on weekends.",
    "Incorporate calcium-rich foods like yogurt or leafy greens into your diet.",
    "Don't skip meals; your body needs energy to deal with hormonal changes.",
    "Listen to your body! Rest when you need to and don't overexert yourself.",
    "Turmeric contains curcumin, which has powerful anti-inflammatory properties.",
    "Acupuncture has been found to help some women with severe menstrual pain.",
    "Cinnamon can help reduce bleeding and relieve pain for some women.",
    "Pineapple contains bromelain, an enzyme that helps relax muscles.",
    "Avoid refined sugars which can cause energy crashes and worsen mood swings.",
    "Keep healthy snacks on hand to manage cravings without the sugar crash.",
    "Stay warm; cold temperatures can sometimes make cramps feel worse.",
    "Try sleeping in the fetal position to take pressure off your abdominal muscles.",
    "Engage in activities that make you happy to boost your mood naturally.",
    "Drink herbal teas like raspberry leaf, traditionally used for uterine health.",
    "Eat foods high in B-vitamins, like whole grains, to help with fatigue.",
    "Limit dairy if you notice it worsens your bloating or digestive issues.",
    "Communicate with your loved ones about how you're feeling for support.",
    "Use a period tracking app to predict when your symptoms might start.",
    "Be kind to yourself; it's okay to take a break from your usual routine.",
    "Try a TENS machine (Transcutaneous Electrical Nerve Stimulation) for pain.",
    "Eat citrus fruits; vitamin C helps your body absorb iron better.",
    "Include probiotic-rich foods like kefir or kimchi to support gut health.",
    "Avoid smoking, as it can worsen PMS symptoms and menstrual cramps.",
    "Try meditation or mindfulness apps to help manage stress and anxiety.",
    "Soak your feet in a warm foot bath to draw blood away from the pelvic area.",
    "Use a hot water bottle—it's a classic remedy for a reason!",
    "Keep your bedroom cool, dark, and quiet to promote better sleep.",
    "Focus on complex carbohydrates like sweet potatoes instead of simple carbs.",
    "A gentle lower back massage from a partner or friend can provide relief.",
    "Stay away from stressful situations if possible when you're feeling sensitive.",
    "Drink warm lemon water in the morning to aid digestion and hydration.",
    "Consider trying a menstrual cup or period underwear for better comfort.",
    "Eat watermelon or cucumber—they have high water content to reduce bloating.",
    "If you experience headaches, try placing a cold pack on your forehead.",
    "Avoid tight waistbands that put pressure on your bloated stomach.",
    "Try 'Cat-Cow' or 'Child's Pose' yoga stretches to relieve pelvic tension.",
    "Incorporate seeds like flax, pumpkin, and sunflower for hormone balance.",
    "Reduce your intake of processed foods which are often high in sodium.",
    "Sip on bone broth for its anti-inflammatory amino acids and minerals.",
    "Focus on good posture; slouching can compress your abdomen and worsen pain.",
    "Take time for a hobby or activity that relaxes your mind.",
    "Discuss severe or debilitating pain with a healthcare provider.",
    "Avoid intense, high-impact workouts if you're feeling extremely fatigued.",
    "Try taking a daytime nap if your nighttime sleep was disrupted.",
    "Use a specialized period pillow or bolster under your knees while sleeping.",
    "Aromatherapy with clary sage or rose oil may help balance emotions.",
    "Eat small amounts of nuts and seeds for a healthy dose of vitamin E.",
    "Try a warm towel wrap around your lower back.",
    "Avoid carbonated drinks as they can add to feelings of gas and bloating.",
    "Eat regular meals to prevent drops in blood sugar that can cause irritability.",
    "Try visualization techniques—imagine the pain melting away.",
    "Incorporate oats into your breakfast; they are soothing and filling.",
    "Avoid excessive screen time before bed to ensure you get restorative sleep.",
    "Drink plenty of decaffeinated fluids throughout the day.",
    "Try acupressure on the webbing between your thumb and index finger.",
    "Eat foods rich in zinc, like pumpkin seeds, to help with period breakouts.",
    "Use a humidifier in your room if you find your sleep is disturbed by dry air.",
    "Practice self-compassion; remind yourself that these feelings are temporary.",
    "Avoid making major life decisions when you are experiencing severe PMS.",
    "Try a gentle, slow-flow pilates routine focused on the core and pelvic floor.",
    "Eat avocado; healthy fats are essential for hormone production.",
    "Spend time in nature; a simple walk in the park can do wonders for your mood.",
    "Try journaling your feelings if you're feeling overwhelmed or anxious.",
    "Keep your feet warm with fuzzy socks to improve overall circulation.",
    "Drink a glass of warm milk with a dash of nutmeg before bed.",
    "Avoid fried foods which can increase inflammation in the body.",
    "Try a digital detox for a few hours to reduce mental stress.",
    "Eat small portions of papaya, which contains an enzyme that aids digestion.",
    "Use an extra pillow to elevate your head if you experience period-related stuffiness.",
    "Focus on your breathing: inhale for 4 seconds, hold for 4, exhale for 4.",
    "Try listening to calming, binaural beats or classical music.",
    "Avoid wearing heavy perfumes if you are experiencing period-related nausea.",
    "Keep a stash of your favorite, comforting (but healthy) snacks accessible.",
    "Try a restorative yoga pose like 'Legs Up the Wall' to improve circulation.",
    "Remember that every cycle is different; what works one month might change the next."
)

@Composable
fun DailyTips() {
    var tipIndex by remember { mutableIntStateOf(java.util.Random().nextInt(WELLNESS_TIPS.size)) }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(10000)
            tipIndex = (tipIndex + 1) % WELLNESS_TIPS.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { tipIndex = (tipIndex + 1) % WELLNESS_TIPS.size },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF1F2)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE4E6))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFE4E6),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WELLNESS TIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedContent(
                    targetState = WELLNESS_TIPS[tipIndex],
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TipAnimation"
                ) { tipText ->
                    Text(
                        text = tipText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C1E21),
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
