// frontend/src/pages/PromptsListPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import Spinner from '../components/Spinner.jsx';
import PromptCard from '../components/PromptCard.jsx'; // <-- IMPORT REUSABLE COMPONENT

const PromptsListPage = () => {
  const [prompts, setPrompts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  
  const PAGE_SIZE = 12;

  const fetchPrompts = useCallback(async (isNewSearch) => {
    const newPage = isNewSearch ? 0 : page + 1;
    isNewSearch ? setLoading(true) : setLoadingMore(true);
    setError(null);

    try {
      const response = await api.get('/prompts', {
        params: { search: searchTerm, page: newPage, size: PAGE_SIZE },
      });
      const { content, totalPages: newTotalPages } = response.data;
      setPrompts(prev => isNewSearch ? content : [...prev, ...content]);
      setPage(newPage);
      setTotalPages(newTotalPages);
    } catch (err) {
      setError('Failed to fetch prompts. Please try again later.');
      console.error('Error fetching prompts:', err);
    } finally {
      isNewSearch ? setLoading(false) : setLoadingMore(false);
    }
  }, [searchTerm, page]);

  useEffect(() => {
    const handler = setTimeout(() => {
      fetchPrompts(true); 
    }, 500);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleLoadMore = () => {
    if (!loadingMore) {
      fetchPrompts(false);
    }
  };

  if (loading && page === 0) {
    return <Spinner message="Loading prompts..." />;
  }

  if (error && prompts.length === 0) {
    return <div className="text-center mt-10 text-red-500">{error}</div>;
  }

  return (
    <div className="container mx-auto p-4 sm:p-6 lg:p-8">
      <div className="flex justify-between items-center mb-4 flex-wrap gap-4">
        <h1 className="text-3xl font-bold">Discover Prompts</h1>
        <Link 
          to="/create-prompt" 
          className="bg-blue-600 text-white font-bold py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          + Create New Prompt
        </Link>
      </div>
      <div className="mb-6">
        <input
          type="text"
          value={searchTerm}
          onChange={handleSearchChange}
          placeholder="Search by title, description, or prompt text..."
          className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {prompts.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* --- REFACTORED TO USE PromptCard COMPONENT --- */}
          {prompts.map((prompt) => (
            <PromptCard key={prompt.id} prompt={prompt} />
          ))}
        </div>
      ) : (
        <div className="text-center mt-10 p-6 bg-white rounded-lg shadow-md">
          <p>{searchTerm ? `No prompts found for "${searchTerm}".` : 'No prompts have been created yet. Be the first!'}</p>
        </div>
      )}

      <div className="text-center mt-8">
        {loadingMore ? (
          <Spinner message="Loading more..." />
        ) : (
          page < totalPages - 1 && (
            <button
              onClick={handleLoadMore}
              className="bg-gray-200 text-gray-800 font-bold py-2 px-6 rounded-md hover:bg-gray-300"
            >
              Load More
            </button>
          )
        )}
      </div>
    </div>
  );
};

export default PromptsListPage;