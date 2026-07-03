package com.capturemate.app.data.repository

import com.capturemate.app.data.local.dao.CaptureDao
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow

class DefaultCaptureRepository(
    private val captureDao: CaptureDao,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureEntity>> = captureDao.observeCaptures()

    override fun observeMemos(): Flow<List<MemoEntity>> = captureDao.observeMemos()
}
