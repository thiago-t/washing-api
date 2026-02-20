package com.thiagotoazza.data.source.company

import com.mongodb.kotlin.client.coroutine.ClientSession
import com.thiagotoazza.data.models.company.Company
import org.bson.BsonValue

interface CompanyDataSource {
    suspend fun getCompanies(): List<Company>
    suspend fun getCompanyById(companyId: String?): Company?
    suspend fun insertCompany(company: Company): BsonValue?
    suspend fun insertCompany(session: ClientSession, company: Company): BsonValue?
    suspend fun updateCompany(company: Company): Boolean
    suspend fun deleteCompany(companyId: String?): Boolean
}