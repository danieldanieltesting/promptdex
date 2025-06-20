// frontend/src/pages/PromptsListPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import Spinner from '../components/Spinner.jsx';

// This helper component is robust and does not need changes.
const StarRating = ({ rating }) => {
  const numericRating = Number(rating) || 0;
  const fullStars = Math.round(numericRating);
  const emptyStars = 5 - fullStars;

  return (
    <div className="flex items-center">
      {Array.from({ length: fullStars }, (_, i) => (
        <span key={`star-full-${i}`} className="text-yellow-500">★</span>
      ))}
      {Array.from({ length: emptyStars }, (_, i) => (
        <span key={`star-empty-${i}`} className="text-gray-300">☆</span>
      ))}
    </div>
  );
};

const PromptsListPage = () => {
  // State for prompts and pagination
  const [prompts, setPrompts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');

  // State for loading and errors
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  
  const PAGE_SIZE = 12;

  // --- REFACTORED API CALL ---
  // Using useCallback to memoize the function for performance.
  const fetchPrompts = useCallback(async (isNewSearch) => {
    // A new search resets the page to 0. Loading more increments the current page.
    const newPage = isNewSearch ? 0 : page + 1;
    // Set the appropriate loading state.
    isNewSearch ? setLoading(true) : setLoadingMore(true);
    setError(null);

    try {
      const response = await api.get('/prompts', {
        params: {
          search: searchTerm,
          page: newPage,
          size: PAGE_SIZE,
        },
      });
      
      const { content, totalPages: newTotalPages } = response.data;
      
      // If it's a new search, replace the prompts. Otherwise, append them.
      setPrompts(prev => isNewSearch ? content : [...prev, ...content]);
      setPage(newPage);
      setTotalPages(newTotalPages);

    } catch (err) {
      setError('Failed to fetch prompts. Please try again later.');
      console.error('Error fetching prompts:', err);
    } finally {
      isNewSearch ? setLoading(false) : setLoadingMore(false);
    }
  }, [searchTerm, page]); // Dependencies for useCallback


  // --- DEBOUNCED SEARCH EFFECT ---
  // This effect runs when the user stops typing in the search box.
  useEffect(() => {
    const handler = setTimeout(() => {
      // Trigger a new search with the current search term.
      fetchPrompts(true); 
    }, 500); // 500ms delay

    // Cleanup function to cancel the timeout if the user types again.
    return () => {
      clearTimeout(handler);
    };
  }, [searchTerm]); // Re-run only when searchTerm changes. Note: fetchPrompts is memoized.


  // --- UI HANDLERS ---
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleLoadMore = () => {
    if (!loadingMore) {
      fetchPrompts(false); // Not a new search, so fetch next page.
    }
  };

  // --- RENDER LOGIC ---

  // Main spinner for initial page load.
  if (loading && page === 0) {
    return <Spinner message="Loading prompts..." />;
  }

  // General error display.
  if (error && prompts.length === 0) {
    return <div className="text-center mt-10 text-red-500">{error}</div>;
  }

  return (
    <div className="container mx-auto p-4 sm:p-6 lg:p-8">
      {/* --- HEADER & SEARCH BAR --- */}
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

      {/* --- PROMPTS GRID --- */}
      {prompts.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {prompts.map((prompt) => (
            <Link 
              to={`/prompts/${prompt.id}`} 
              key={prompt.id} 
              className="flex flex-col p-6 bg-white rounded-lg border border-gray-200 shadow-md hover:bg-gray-100 transition duration-300"
            >
              <div className="flex-grow">
                <h5 className="mb-2 text-2xl font-bold tracking-tight text-gray-900 break-words">{prompt.title}</h5>
                <p className="font-normal text-gray-700">{prompt.description}</p>
              </div>
              <div className="mt-4 pt-4 border-t border-gray-200 flex justify-between items-center">
                <p className="text-sm text-gray-500">By: {prompt.authorUsername}</p>
                <StarRating rating={prompt.averageRating} />
              </div>
            </Link>
          ))}
        </div>
      ) : (
        // --- DYNAMIC 'NO RESULTS' MESSAGE ---
        <div className="text-center mt-10 p-6 bg-white rounded-lg shadow-md">
          <p>{searchTerm ? `No prompts found for "${searchTerm}".` : 'No prompts have been created yet. Be the first!'}</p>
        </div>
      )}

      {/* --- LOAD MORE BUTTON & SPINNER --- */}
      <div className="text-center mt-8">
        {loadingMore ? (
          <Spinner message="Loading more..." />
        ) : (
          page < totalPages - 1 && (
            <button
              onClick={handleLoadMore}
              className="bg-gray-200 text-gray-800 font-bold py-2 px-6 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-400"
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