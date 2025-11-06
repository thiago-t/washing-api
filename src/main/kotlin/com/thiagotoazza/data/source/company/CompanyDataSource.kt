package com.thiagotoazza.data.source.company

import com.thiagotoazza.data.models.company.Company

interface CompanyDataSource {
    suspend fun getCompanies(): List<Company>
    suspend fun getCompanyById(companyId: String?): Company?
    suspend fun insertCompany(company: Company): Boolean
    suspend fun updateCompany(company: Company): Boolean
    suspend fun deleteCompany(companyId: String?): Boolean
}