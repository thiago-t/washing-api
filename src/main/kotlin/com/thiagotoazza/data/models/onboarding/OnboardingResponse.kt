package com.thiagotoazza.data.models.onboarding

import com.thiagotoazza.data.models.company.CompanyResponse
import com.thiagotoazza.data.models.user.UserResponse

data class OnboardingResponse(
    val user: UserResponse,
    val company: CompanyResponse
)
